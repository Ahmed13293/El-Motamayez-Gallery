importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.0/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "AIzaSyD5T5Rw7VfoWP3A2PhDqaqrOH9oEtaEtzc",
  authDomain: "el-motamayz-library.firebaseapp.com",
  projectId: "el-motamayz-library",
  storageBucket: "el-motamayz-library.firebasestorage.app",
  messagingSenderId: "1013516234653",
  appId: "1:1013516234653:web:4755d52ca8c8cae54c8974",
});

const messaging = firebase.messaging();

// Handle background messages (app not in foreground)
messaging.onBackgroundMessage((payload) => {
  const title = payload.notification?.title || 'مكتبة المتميز';
  const options = {
    body: payload.notification?.body || '',
    icon: '/icon.png',
    badge: '/icon.png',
    dir: 'rtl',
    lang: 'ar',
    data: { navigateTo: 'orders' },
  };
  self.registration.showNotification(title, options);
});

// Navigate to orders tab when notification is clicked
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      // If the app is already open, post a message to it
      for (const client of clientList) {
        if ('focus' in client) {
          client.postMessage({ navigateTo: 'orders' });
          return client.focus();
        }
      }
      // Otherwise open it with a query param
      return clients.openWindow('/?navigate=orders');
    })
  );
});
