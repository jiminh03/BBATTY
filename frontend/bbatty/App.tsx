import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { ThemeProvider } from './src/shared/styles/ThemeContext';
import ChatNavigator from './src/navigation/stacks/ChatNavigator';

const App = () => {
  return (
    <ThemeProvider>
      <NavigationContainer>
        <ChatNavigator />
      </NavigationContainer>
    </ThemeProvider>
  );
};

export default App;