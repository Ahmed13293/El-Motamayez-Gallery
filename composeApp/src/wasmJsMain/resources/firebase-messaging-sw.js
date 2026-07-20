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

// Background messages: the browser auto-displays the notification from the FCM
// notification payload. We only need to attach our custom data so the click
// handler can navigate correctly.
messaging.onBackgroundMessage((payload) => {
  // No-op for display — handled automatically by the browser.
  // The notificationclick listener below uses event.notification.data.
});

// Navigate to orders tab when notification is clicked
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.postMessage({ navigateTo: 'orders' });
          return client.focus();
        }
      }
      return clients.openWindow(self.registration.scope + '?navigate=orders');
    })
  );
});
