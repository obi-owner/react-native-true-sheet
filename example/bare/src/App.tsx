import { NavigationContainer } from '@react-navigation/native';
import { ReanimatedTrueSheetProvider } from '@lodev09/react-native-true-sheet/reanimated';

import { RootNavigator } from './navigators';
import { KeyboardProvider } from "react-native-keyboard-controller";

const App = () => {
  return (
    <ReanimatedTrueSheetProvider>
      <NavigationContainer>
        <KeyboardProvider>
          <RootNavigator />
        </KeyboardProvider>
      </NavigationContainer>
    </ReanimatedTrueSheetProvider>
  );
};

export default App;
