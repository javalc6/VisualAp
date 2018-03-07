@rem build 
del /s *.class
del /s *.bak
javac graph\*.java
javac visualap\*.java
jar cmf build.mf visualap.jar visualap\*.* graph\*.* property\*.* parser\*.* common\*.*
jar cmf setup.mf visualap-setup.jar visualap.jar visualap\Setup.class beans\*.jar doc\*.pdf readme.txt license.txt ding.wav *.jpg *.vas *.mf build.bat visualap.bat visualap.sh