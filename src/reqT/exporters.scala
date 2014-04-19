package reqT
package exporters

trait Exporter {
  //utilities:
  val q: String = '\"'.toString
  val q3: String = q*3
  val nl = "\n"
  def nlLitteral = """\n"""
  def levelTab(level: Int): String   = " " * (level * Settings.intentSpacing)
  def indent(n: Int): String = levelTab(n)
  
  //stubbs to override:
  def preamble(m: Model): String = ""
  def ending(m: Model): String = ""
  def body(m: Model): String  
  def apply(m: Model): String = preamble(m) + body(m) + ending(m)
}

case object Simple extends Exporter {
  override def body(m: Model): String = m.toStringSimple
}

trait ModelToString extends Exporter {
  def emptyModelString: String = "()"

  def indentCheck(m: Model, path: NodePath): String = {
    val space = "\n" + indent(path.level + 1)
    if (m.toStringBody.length > (Settings.lineLength - space.length)) space else ""
  }
  
  def modelPre: String = "("  
  def modelPost(m: Model, path: NodePath): String = ")"  
  def elemSep: String = ", "
    
  def exportModel(sub: Model, path: NodePath): String = 
    if (sub.isEmpty) ""
    else if (sub.size == 1) exportElem(sub.elems.head, sub, path)
    else sub.elems.map(exportElem(_, sub, path)).mkString(elemSep) 
  
  def exportElem(elem: Elem, sub: Model, path: NodePath): String = elem match {
    case NoElem => ""
    case e: Entity => indentCheck(sub, path) + exportEntity(e, path / e) 
    case a: Attribute[_] => indentCheck(sub, path) + exportAttribute(a, path / a)
    case r: Relation => 
      val isSimple = (r.tail.size == 1 && r.tail.elems.head.isNode)
      indentCheck(sub, path) + exportHead(r.head, path / r.head) + 
        ( if (!isSimple) modelPre else "" ) + 
          exportModel(r.tail, path / r.head) + 
            ( if (!isSimple) modelPost(r.tail, path) else "" )
  }
    
  def exportHead(h: Head, path: NodePath): String = 
    exportEntity(h.entity, path) + " " + h.link + " "
  def exportEntity(e: Entity, path: NodePath): String = e.toString
  def exportAttribute[T](a: Attribute[T], path: NodePath): String =  a.toString
  
  override def preamble(m: Model): String = "Model("
  override def ending(m: Model): String = ")"
  override def body(m: Model): String = exportModel(m, /)
}


trait NewLineEnding { self: ModelToString =>
  override def modelPost(m: Model, path: NodePath) = indentCheck(m, path) + ")"
  override def ending(m: Model) = if (m.toStringBody.length > Settings.lineLength) "\n)" else ")" 
}  

case object PrettyCompact extends ModelToString 
case object Pretty extends ModelToString with NewLineEnding

trait ScalaGenerators { self: ModelToString =>
  override def exportEntity(e: Entity, path: NodePath): String = e.toScala
  override def exportAttribute[T](a: Attribute[T], path: NodePath): String =  a.toScala
}

case object ScalaCompact extends ModelToString with ScalaGenerators 
case object Scala extends ModelToString with ScalaGenerators with NewLineEnding

trait GraphVizGenerator extends Exporter {
  def formats = """
  compound=true;overlap=false;rankdir=LR;clusterrank=local;
  node [fontname="Sans", fontsize=9];
  edge [fontname="Sans", fontsize=9];
"""
  
  def style(elem: Elem): String = elem match {
    case e: Entity => 
      val (row1, row2) = (e.myType, e.id) 
      s" [label=$q$row1$nlLitteral$row2$q, shape=box]"
    case a: Attribute[_] => 
      val (row1, row2) = (a.myType, a.value) 
      s" [label=$q$row1$nlLitteral$row2$q, shape=box, style=rounded]"
    case _ => ""
  }
  
  def node(e: Elem, path: NodePath): String = s"  $q$path$e$q"
  
  def singleSubnodeLink(from: Entity, link: RelationType, to: Elem, path: NodePath): String = 
    indent(path.level) + node(from, path) + style(from) + ";\n" +
    indent(path.level) + node(to, path/from) + style(to) + ";\n" +
    indent(path.level) + node(from, path) + " -> " + node(to, path/from) + s"[label=$link]" + ";\n"
      
  def subGraphPre(from: Entity, link: RelationType, to: Elem, path: NodePath): String =
    indent(path.level) + node(from, path) + style(from) + ";\n" +
    indent(path.level) + node(from, path) + " -> " + node(to, path/from) + 
    s" [label=$link, lhead=${q}cluster_$from$q]" + ";\n" +
    indent(path.level) + s"  subgraph ${q}cluster_$from$q { \n"

  def exportModel(m: Model, path: NodePath): String = m.collect {
    case n: Node => indent(path.level) + node(n, path) + style(n) +";\n"
    case Relation(e1,l1,sub) => sub match {
      case Model() => indent(path.level) + node(e1, path) + style(e1) +";\n" 
      case Model(e2) if e2.isNode => singleSubnodeLink(e1, l1, e2, path)
      case Model(Relation(e2, _ , Model())) => singleSubnodeLink(e1, l1, e2, path)
      case Model(Relation(e2, l2, sub2)) if sub2.tip.size == 1 => 
        singleSubnodeLink(e1, l1, e2, path) + 
        singleSubnodeLink(e2, l2, sub2.tip.elems.head, path/e1) +
        exportModel(sub2, path/e1/e2)
      case _ => 
        subGraphPre(e1, l1, sub.tip.elems.head, path) +
        exportModel(sub, path/e1)  + indent(path.level + 1) + "}\n"
    }
  } .mkString
    
  override def preamble(m: Model): String = s"""digraph ${q}reqT.Model${q} { $nl$formats$nl"""
  override def ending(m: Model): String = "\n}"
  override def body(m: Model): String = exportModel(m.reverse,/)
}

case object NestedGV extends GraphVizGenerator  

trait GraphMLGenerator extends Exporter {
  def formats = """
  compound=true;overlap=false;rankdir=LR;clusterrank=local;
  node [fontname="Sans", fontsize=9];
  edge [fontname="Sans", fontsize=9];
"""
  
  def style(elem: Elem): String = elem match {
    case e: Entity => 
      val (row1, row2) = (e.myType, e.id) 
      s" [label=$q$row1$nlLitteral$row2$q, shape=box]"
    case a: Attribute[_] => 
      val (row1, row2) = (a.myType, a.value) 
      s" [label=$q$row1$nlLitteral$row2$q, shape=box, style=rounded]"
    case _ => ""
  }
  
  def node(e: Elem, path: NodePath): String = s"  $q$path$e$q"
  
  def singleSubnodeLink(from: Entity, link: RelationType, to: Elem, path: NodePath): String = 
    indent(path.level) + node(from, path) + style(from) + ";\n" +
    indent(path.level) + node(to, path/from) + style(to) + ";\n" +
    indent(path.level) + node(from, path) + " -> " + node(to, path/from) + s"[label=$link]" + ";\n"
      
  def subGraphPre(from: Entity, link: RelationType, to: Elem, path: NodePath): String =
    indent(path.level) + node(from, path) + style(from) + ";\n" +
    indent(path.level) + node(from, path) + " -> " + node(to, path/from) + 
    s" [label=$link, lhead=${q}cluster_$from$q]" + ";\n" +
    indent(path.level) + s"  subgraph ${q}cluster_$from$q { \n"

  def exportModel(m: Model, path: NodePath): String = m.collect {
    case n: Node => indent(path.level) + node(n, path) + style(n) +";\n"
    case Relation(e1,l1,sub) => sub match {
      case Model() => indent(path.level) + node(e1, path) + style(e1) +";\n" 
      case Model(e2) if e2.isNode => singleSubnodeLink(e1, l1, e2, path)
      case Model(Relation(e2, _ , Model())) => singleSubnodeLink(e1, l1, e2, path)
      case Model(Relation(e2, l2, sub2)) if sub2.tip.size == 1 => 
        singleSubnodeLink(e1, l1, e2, path) + 
        singleSubnodeLink(e2, l2, sub2.tip.elems.head, path/e1) +
        exportModel(sub2, path/e1/e2)
      case _ => 
        subGraphPre(e1, l1, sub.tip.elems.head, path) +
        exportModel(sub, path/e1)  + indent(path.level + 1) + "}\n"
    }
  } .mkString
    
  /*** Verbose header needed */
  override def preamble(m: Model): String = s"""<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<graphml xmlns="http://graphml.graphdrawing.org/xmlns" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:y="http://www.yworks.com/xml/graphml" xmlns:yed="http://www.yworks.com/xml/yed/3" xsi:schemaLocation="http://graphml.graphdrawing.org/xmlns http://www.yworks.com/xml/schema/graphml/1.1/ygraphml.xsd">
<!--Tailored for yFiles for Java 2.9-->
	<key for="graphml" id="d0" yfiles.type="resources"/>
	<key for="port" id="d1" yfiles.type="portgraphics"/>
	<key for="port" id="d2" yfiles.type="portgeometry"/>
	<key for="port" id="d3" yfiles.type="portuserdata"/>
	<key attr.name="url" attr.type="string" for="node" id="d4"/>
	<key attr.name="description" attr.type="string" for="node" id="d5"/>
	<key for="node" id="d6" yfiles.type="nodegraphics"/>
	<key attr.name="Description" attr.type="string" for="graph" id="d7"/>
	<key attr.name="url" attr.type="string" for="edge" id="d8"/>
	<key attr.name="description" attr.type="string" for="edge" id="d9"/>
	<key for="edge" id="d10" yfiles.type="edgegraphics"/>
"""
      
  override def ending(m: Model): String = """ 
	<data key="d0">
		<y:Resources/>
	</data>
</graphml>
  	  """
  	  
  override def body(m: Model): String = exportModel(m.reverse,/)
}

case object NestedGML extends GraphMLGenerator


















