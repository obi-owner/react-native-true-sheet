import { useRef } from 'react';
import { StyleSheet, View, Text } from 'react-native';
import { useRouter } from 'expo-router';
import { type TrueSheet } from '@lodev09/react-native-true-sheet';

import { BLUE, GAP, SPACING, LIGHT_GRAY } from '@example/shared/utils';
import { Button } from '@example/shared/components';
import { AutoScrollableSheet } from '@example/shared/sheets';

export default function AutoScrollableTest() {
  const router = useRouter();
  const sheetRef = useRef<TrueSheet>(null);

  return (
    <View style={styles.content}>
      <Button text="Go Back" onPress={() => router.back()} />

      <View style={styles.infoContainer}>
        <Text style={styles.title}>Auto Detent + Scrollable Test</Text>
        <Text style={styles.description}>
          This screen tests the auto detent sizing with scrollable content on Android. The sheet
          should automatically size to fit its content when using the 'auto' detent.
        </Text>
        <Text style={styles.description}>
          Use the controls in the sheet header to toggle between auto/fixed detents, enable/disable
          scrollable, and adjust the number of items to verify dynamic sizing.
        </Text>
      </View>

      <Button text="Open Auto Scrollable Sheet" onPress={() => sheetRef.current?.present()} />

      <AutoScrollableSheet ref={sheetRef} itemCount={3} />
    </View>
  );
}

const styles = StyleSheet.create({
  content: {
    flex: 1,
    backgroundColor: BLUE,
    padding: SPACING,
    gap: GAP,
  },
  infoContainer: {
    flex: 1,
    justifyContent: 'center',
  },
  title: {
    color: '#fff',
    fontSize: 24,
    fontWeight: 'bold',
    marginBottom: SPACING,
    textAlign: 'center',
  },
  description: {
    color: LIGHT_GRAY,
    fontSize: 16,
    lineHeight: 24,
    textAlign: 'center',
    marginBottom: SPACING,
  },
});
