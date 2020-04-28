Updater
=======
A built-in application which assists user in the downloading and flashing of OTA packages on device by using the recovery.

Server requirements
-------------------
The app sends `GET` requests to the URL defined by the `updater_server_url`
resource (or the `exthm.updater.uri` system property) and expects as response
a directory with the following directory tree:
```
update_script
    │  notice.json
    │  
    ├─{device}
    │      update.json
    │      
    ├─{device2}
    │      update.json
    │      
    ├─{device3}
    │      update.json
    │
    └─{device ...}
```
The `{device}`s attribute is the string to be compared with the device name.
   
The `update.json`s expects as response a json file with the following structure:
```json
{
  "response": [
    {
      "name": "{os_name} 1.0",
      "device": "example",
      "packagetype": "full",
      "requirement": 0,
      "changelog": "VXBkYXRlJTIwdG8lMjBleFRIbVVJJTIwMS4w",
      "timestamp": 1590764400,
      "filename": "ota-package.zip",
      "sha1": "d287caa0c7b7aba365ecb9c6b818cab70bfbcc70c75e779ea489df487da04bcc",
      "romtype": "stable",
      "size": 1024000000,
      "url": "https://example.com/ota-package.zip",
      "imageurl": "https://avatars2.githubusercontent.com/u/53985366?s=200&v=4",
      "version": "1.0"
    }
  ]
}
```
The `name` attribute is the name(description) of this update.The `{os_name}` will be replaced to `R.strings.os_name`  
The `device` attribute is the string to be compared with the device name.  
The `packagetype` attribute is the type of the update (support: full, incremental).  
The `changelog` attribute is the base64 encoded changelogs of this update.  
The `requirement` attribute is the string to be compared with the system build timestamp (for incremental updates only).  
The `timestamp` attribute is the build date expressed as UNIX timestamp.  
The `filename` attribute is the name of the file to be downloaded.  
The `sha1` attribute is the sha1sum result of the update file.  
The `romtype` attribute is the string to be compared with the `ro.exthm.releasetype` property.  
The `size` attribute is the size of the update expressed in bytes.  
The `url` attribute is the URL of the file to be downloaded.  
The `version` attribute is the string to be compared with the `ro.exthm.build.version` property.  
The `imageurl` attribute is the URL of the image to be shown on the back of the update's cardview. 

Additional attributes are ignored.

The `notice.json` expects as response a json file with the following structure:
```json
{
  "response": [
    {
      "title": "SSUyN20lMjBoZXJlJTJDZXhUSG1VSSUyMQ==",
      "text": "SVNOaW5nJTIwaGF2ZSUyMHZpc2l0ZWQlMjB0aGlzJTIwcGxhY2UlMjAlM0ElMjklMjA=",
      "id": "0",
      "imageurl": "https://avatars2.githubusercontent.com/u/53985366?s=200&v=4"
    }
  ]
}
```
The `title` attribute is the base64 encoded title of this notice.  
The `text` attribute is the base64 encoded text of this notice.  
The `id` attribute is the order of this notice.  
The `imageurl` attribute is the URL of the image to be shown on the back of the update's cardview. 

Additional attributes are ignored.

Build with Android Studio
-------------------------
Updater requires access to the system API, hence it cannot be built if you only have the public SDK.  
First,You would have to generate the libraries with all the required classes.  
The application also needs elevated privileges, so you need to sign it with the right key to replace the previous one located in the system partition. To do this:
 - Place this directory anywhere in the Android source tree
 - Generate a keystore and keystore.properties using `gen-keystore.sh`
 - You will only need to do it once, unless Android Studio can't find the required symbols.
 - Build the dependencies running `make UpdaterStudio` from the root of the
   exTHmUI(Important:We have used a custom API in the Class:android.os.RecoverySystem) source tree. This command will add the needed libraries in
   `{the_root_of_updater}/system_libs/`.

You need to do the above once, unless Android Studio can't find some symbol.
In this case, rebuild the system libraries with `make UpdaterStudio`.
