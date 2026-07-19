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
  };
  self.registration.showNotification(title, options);
});
