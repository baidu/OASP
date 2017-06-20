# Introduction

The OASPClient directory contains a reference implementation of the OASP client app.

It simply scans all the installed non-system apps, and verifies their OASP status (if the app supports OASP).


# Building

The IDSIG signature is verified before being submitted to the remote server for attestation, and this part is actually similar to how Android verifies the apk signature (META-INF/*). To simplify the code base, I directly reused the code from android framework. More specifically, during my development, I copied the framework code from the [AndroidVTS](https://github.com/AndroidVTS/android-vts) project. 

Therefore, in order to build the app, you need to first clone AndroidVTS’ android/framework directory (I included the exact one that I used in my development, as the android_framework.zip file). You need to copy the android/framework directory to OASPClient/app/src/main/java (so that it becomes OASPClient/app/src/main/java/android/framework).

Then replace the following two files using the ones that I provided in the RevisedClasses directory:

* OASPClient/app/src/main/java/android/framework/util/jar/JarFile.java
* OASPClient/app/src/main/java/android/framework/util/jar/JarVerifier.java

My modifications of these two files include:
1. adjusting the verification of the META-INF directory to the IDSIG directory; 
2. adding SHA-256 as one of the digest algorithms for JarVerifier.

You should be able to build the app afterwards.


# Running

Please don’t forget to add your server’s certificate into your device’s trust list if it’s self-signed. The app does not explicitly ignore SSL connection errors.

