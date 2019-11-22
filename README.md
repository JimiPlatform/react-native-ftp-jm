
# react-native-react-native-ftp-jm

## Getting started

`$ npm install react-native-react-native-ftp-jm --save`

### Mostly automatic installation

`$ react-native link react-native-react-native-ftp-jm`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-react-native-ftp-jm` and add `RNReactNativeFtpJm.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNReactNativeFtpJm.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.jm.ftp.RNReactNativeFtpJmPackage;` to the imports at the top of the file
  - Add `new RNReactNativeFtpJmPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-react-native-ftp-jm'
  	project(':react-native-react-native-ftp-jm').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-react-native-ftp-jm/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-react-native-ftp-jm')
  	```


## Usage
```javascript
import RNReactNativeFtpJm from 'react-native-react-native-ftp-jm';

// TODO: What to do with the module?
RNReactNativeFtpJm;
```
  