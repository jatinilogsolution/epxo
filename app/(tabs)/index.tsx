import React, { useEffect, useState } from 'react';
import { View, Switch, Text } from 'react-native';
import UHFBarcodeModule from '../../UHFBarcodeModulets';

const HomeScreen = () => {
  const [isBarcode, setIsBarcode] = useState(true);
  const [scannedData, setScannedData] = useState('');

  useEffect(() => {
    const barcodeListener = UHFBarcodeModule.addListener(
      'onBarcodeScanned',
      event => {
        console.log('event', event);
        setScannedData(event.scannedData);
      },
    );

    // const uhfListener = UHFBarcodeModule.addListener('onUHFScanned', event => {
    //   setScannedData(event.scannedData);
    // });

    return () => {
      UHFBarcodeModule.removeListener(barcodeListener);
      // UHFBarcodeModule.removeListener(uhfListener);
    };
  }, []);

  const handleToggle = (value: boolean | ((prevState: boolean) => boolean)) => {
    setIsBarcode(value);
    UHFBarcodeModule.toggleScanMode(value, (message: any) => {
      console.log(message);
    });
  };

  return (
    <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center' }}>
      <Switch value={isBarcode} onValueChange={handleToggle} />
      <Text>{isBarcode ? 'Barcode Scanner' : 'UHF Scanner'}</Text>
      <Text>Scanned Data: {scannedData}</Text>
    </View>
  );
};

export default HomeScreen;
