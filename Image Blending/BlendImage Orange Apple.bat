javac -cp ..\common; BlendImage.java
java -cp ..\common; BlendImage orange.png apple.png orange_weight.png apple_weight.png
del *.class
del ..\common\*.class