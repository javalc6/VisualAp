@rem build 
del /s *.class
del /s *.bak
dir /s /b src\*.java > sources.txt
javac -d classes -sourcepath src -g @sources.txt
del sources.txt
xcopy /s src\*.png classes\
xcopy /s src\*.jpg classes\
xcopy /s src\*.gif classes\
jar cmf build.mf visualap.jar src\visualap\*.* src\graph\*.* src\property\*.* src\parser\*.* src\common\*.* src\json\*.* -C classes .
jar cmf setup.mf visualap-setup.jar visualap.jar beans\*.jar doc\*.pdf readme.txt license.txt ding.wav *.jpg examples\*.vas *.mf build.bat visualap.bat visualap.sh -C classes visualap\Setup.class 
