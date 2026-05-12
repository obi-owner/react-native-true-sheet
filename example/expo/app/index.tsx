import { useRouter } from 'expo-router';

import { MapScreen } from '@example/shared/screens';
import { Map } from '@example/shared/components';
import { StyleSheet, View, type StyleProp, type ViewStyle } from 'react-native';

const HAS_MAPS_KEY = !!process.env.GOOGLE_MAPS_API_KEY;

export default function Index() {
  const router = useRouter();

  return (
    <MapScreen
      MapComponent={HAS_MAPS_KEY ? Map : MapPlaceholder}
      onNavigateToModal={() => router.push('/modal')}
      onNavigateToSheetStack={() => router.push('/sheet')}
      onNavigateToTest={() => router.push('/test')}
      onNavigateToTestStack={() => router.push('/test-stack')}
    />
  );
}

// Renders a blank view when GOOGLE_MAPS_API_KEY isn't set, so the rest of the
// screen (including the TrueSheet under test) still loads.
const MapPlaceholder = ({ style }: { style?: StyleProp<ViewStyle> }) => (
  <View style={[styles.placeholder, style]} />
);

const styles = StyleSheet.create({
  placeholder: {
    flex: 1,
    backgroundColor: '#0a4da0',
  },
});
