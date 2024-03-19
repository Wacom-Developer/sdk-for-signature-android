# Wacom Ink SDK for signature - Android

## Version 2.3

## History

* 2.3 27 March 2020

   * Added SignatureCaptureView as extension of TextureView
   * Added SignatureCaptureSurfaceView as extension of SurfaceView
   * Added ISO encryption support
   * Extract WILL SDK to make it compatible with other Wacom SDKs
   * Fixed bug in hash with really big files
   * Fixed bug with rendering pen size 

* 2.2 22 February  2019

   * Fixed problem when pressing spen stylus button while signing
   * Fixed problem when using bambbo stylus on touch mode
   * Fixed production licensing problems.
 
* 2.1 22 June 2018

   * Added support for Encryption, required bouncy castle libraries for using encryption
   * Added ISO standard support
   
* 2.0 02 March 2018

    *	now an Android Studio Project instead of Eclipse
    *	the SDK is now an aar library instead of a single apk.
      This implies that the user can include it in a Project without installation
    *	the JWT license format is now supported
    *	due to vulnerability issues updated to the latest version of libpng
    * now requires the library 'jose4j'
      add the following in the Android Studio Project:
      compile group: 'org.bitbucket.b_c', name: 'jose4j', version: '0.5.2'
    * Revised zip file format

