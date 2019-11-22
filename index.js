
import { NativeModules } from 'react-native';

const { JMFTPSyncFileManager,JMUDPScoketManager} = NativeModules;

// export default JMFTPSyncFileManager;
export default {
    JMFTPSyncFileManager,
    JMUDPScoketManager,
};