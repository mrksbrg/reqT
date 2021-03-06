@echo OFF
echo Clean compile, removing directory target\reqT
rmdir /Q target\reqT
set _jarfilename=reqT.jar
echo Compiling reqT ...  %TIME%
call scalac -feature -deprecation -cp "lib\*" -d target src/main/scala/*
if %ERRORLEVEL% NEQ 0 goto error
echo Compilation ready!  %TIME%
echo Packaging reqT into jar file: %_jarfilename% 
echo Start packaging ... %TIME%
call jar cfe %_jarfilename% reqt.start -C target/ .
if %ERRORLEVEL% NEQ 0 goto error
echo Packaging ready!    %TIME%
if not exist "%USERPROFILE%\.kojo\lite\libk" goto checklibdir
echo Copying %_jarfilename% to "%USERPROFILE%\.kojo\lite\libk\."
copy /Y %_jarfilename% "%USERPROFILE%\.kojo\lite\libk\."
echo Copying "reqTinit.kojo" to "%USERPROFILE%\.kojo\lite\initk\."
copy /Y "reqTinit.kojo" "%USERPROFILE%\.kojo\lite\initk\."
:checklibdir
if exist "%USERPROFILE%\reqT\lib" goto checkbindir
mkdir "%USERPROFILE%\reqT\lib"
:checkbindir
if exist "%USERPROFILE%\reqT\bin" goto copyreqt
mkdir "%USERPROFILE%\reqT\bin"
:copyreqt
echo Copying %_jarfilename% to %USERPROFILE%\reqT\lib\
copy /Y %_jarfilename% "%USERPROFILE%\reqT\lib\."
echo Copying reqt.cmd to %USERPROFILE%\reqT\bin\
copy /Y reqt.cmd "%USERPROFILE%\reqT\bin\."
echo If %USERPROFILE%\reqT\bin is in your Path you can run reqt as a command
goto end
:error
echo Error level: %ERRORLEVEL%
:end
