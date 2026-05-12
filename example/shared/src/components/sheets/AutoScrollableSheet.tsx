import { TrueSheet, type TrueSheetProps } from '@lodev09/react-native-true-sheet';
import { forwardRef, useState } from 'react';
import { FlatList, Platform, StyleSheet, Switch, Text, View } from 'react-native';

import { BORDER_RADIUS, DARK, DARK_GRAY, GAP, LIGHT_GRAY, SPACING, times } from '../../utils';
import { DemoContent } from '../DemoContent';
import { Header } from '../Header';
import { Spacer } from '../Spacer';

interface AutoScrollableSheetProps extends TrueSheetProps {
  itemCount?: number;
}

export const AutoScrollableSheet = forwardRef<TrueSheet, AutoScrollableSheetProps>(
  ({ itemCount = 3, ...props }, ref) => {
    const [useAutoDetent, setUseAutoDetent] = useState(true);
    const [scrollableEnabled, setScrollableEnabled] = useState(true);
    const [count, setCount] = useState(itemCount);

    return (
      <TrueSheet
        ref={ref}
        detents={useAutoDetent ? ['auto', 1] : [0.5, 1]}
        name="auto-scrollable"
        scrollable={scrollableEnabled}
        backgroundColor={Platform.select({ android: DARK })}
        header={
          <Header>
            <View style={styles.controls}>
              <View style={styles.controlRow}>
                <Text style={styles.controlLabel}>Auto Detent</Text>
                <Switch value={useAutoDetent} onValueChange={setUseAutoDetent} />
              </View>
              <View style={styles.controlRow}>
                <Text style={styles.controlLabel}>Scrollable</Text>
                <Switch value={scrollableEnabled} onValueChange={setScrollableEnabled} />
              </View>
              <View style={styles.controlRow}>
                <Text style={styles.controlLabel}>Items: {count}</Text>
                <View style={styles.countButtons}>
                  <Text
                    style={styles.countButton}
                    onPress={() => setCount((c) => Math.max(1, c - 1))}
                  >
                    -
                  </Text>
                  <Text style={styles.countButton} onPress={() => setCount((c) => c + 1)}>
                    +
                  </Text>
                </View>
              </View>
            </View>
          </Header>
        }
        onDidDismiss={() => console.log('AutoScrollableSheet dismissed!')}
        onDidPresent={() => console.log('AutoScrollableSheet presented!')}
        {...props}
      >
        <FlatList
          data={times(count, (i) => i)}
          contentContainerStyle={{
            paddingBottom: SPACING,
            paddingTop: SPACING,
            backgroundColor: 'blue',
          }}
          style={{
            // padding: SPACING * 2,
            backgroundColor: 'green',
          }}
          renderItem={({ item }) => <DemoContent color={DARK_GRAY} text={`Item #${item}`} />}
          ItemSeparatorComponent={Spacer}
        />
      </TrueSheet>
    );
  }
);

AutoScrollableSheet.displayName = 'AutoScrollableSheet';

const styles = StyleSheet.create({
  content: {
    // padding: SPACING,
    // paddingBottom: 0,
    backgroundColor: 'red',
  },
  controls: {
    gap: GAP / 2,
  },
  controlRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  controlLabel: {
    color: LIGHT_GRAY,
    fontSize: 14,
  },
  countButtons: {
    flexDirection: 'row',
    gap: SPACING,
  },
  countButton: {
    color: '#fff',
    fontSize: 20,
    fontWeight: 'bold',
    paddingHorizontal: SPACING,
  },
  infoBox: {
    backgroundColor: 'rgba(55, 132, 215, 0.3)',
    borderRadius: BORDER_RADIUS,
    padding: SPACING,
  },
  infoTitle: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
    marginBottom: GAP / 2,
  },
  infoText: {
    color: LIGHT_GRAY,
    fontSize: 14,
    fontFamily: Platform.select({ ios: 'Menlo', android: 'monospace' }),
  },
});
