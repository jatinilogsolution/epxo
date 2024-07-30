// UHFBarcodeModule.js
import { NativeModules, NativeEventEmitter } from 'react-native';

const { UHFBarcodeModule } = NativeModules;
const UHFBarcodeModuleEmitter = new NativeEventEmitter(UHFBarcodeModule);

export default {
  toggleScanMode: (isBarcode: any, callback: any) => {
    UHFBarcodeModule.toggleScanMode(isBarcode, callback);
  },
  addListener: (eventName: string, handler: (event: any) => void) => {
    return UHFBarcodeModuleEmitter.addListener(eventName, handler);
  },
  removeListener: (subscription: { remove: () => void }) => {
    subscription.remove();
  },
};
