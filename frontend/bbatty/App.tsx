// App.tsx
import React from 'react';
import { View, StyleSheet, SafeAreaView } from 'react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SendMessageForm } from './src/features/send-message/ui/SendMessageForm';

// QueryClient 생성
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
    mutations: {
      retry: false,
    },
  },
});

const App = () => {
  const handleSend = (message: string) => {
    console.log('Message sent from App:', message);
  };

  return (
    <QueryClientProvider client={queryClient}>
      <SafeAreaView style={styles.container}>
        <View style={styles.content}>
          <SendMessageForm
            roomId="test-room-123"
            placeholder="테스트 메시지를 입력하세요..."
            maxLength={200}
            onSend={handleSend}
          />
        </View>
      </SafeAreaView>
    </QueryClientProvider>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  content: {
    flex: 1,
    justifyContent: 'flex-end',
    padding: 16,
  },
});

export default App;