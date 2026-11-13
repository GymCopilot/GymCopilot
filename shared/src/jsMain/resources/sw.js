const CACHE_NAME = 'gymcopilot-v10';
const ASSETS = [
    './',
    './index.html',
    './manifest.json',
    './icon-192.png',
    './icon-512.png',
    './skiko.js',
    './shared.js'
];

self.addEventListener('install', (event) => {
    event.waitUntil(
        caches.open(CACHE_NAME).then((cache) => {
            return cache.addAll(ASSETS).catch(() => {});
        })
    );
    self.skipWaiting();
});

self.addEventListener('activate', (event) => {
    event.waitUntil(
        caches.keys().then((keys) => {
            return Promise.all(
                keys.map((key) => {
                    if (key !== CACHE_NAME) {
                        return caches.delete(key);
                    }
                })
            );
        }).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (event) => {
    const url = new URL(event.request.url);

    // For Google Script API requests / dynamic data: Network first, fallback to Cache
    if (url.hostname.includes('script.google.com') || url.pathname.includes('exec')) {
        event.respondWith(
            fetch(event.request)
                .then((response) => {
                    if (response && response.status === 200) {
                        const responseClone = response.clone();
                        caches.open(CACHE_NAME).then((cache) => {
                            cache.put(event.request, responseClone);
                        });
                    }
                    return response;
                })
                .catch(() => {
                    return caches.match(event.request).then((cachedResponse) => {
                        return cachedResponse || new Response(JSON.stringify({ status: "success", data: [], offline: true }), {
                            headers: { 'Content-Type': 'application/json' }
                        });
                    });
                })
        );
    } else {
        // For static assets: Cache first, fallback to Network
        event.respondWith(
            caches.match(event.request)
                .then((cachedResponse) => {
                    return cachedResponse || fetch(event.request).then((response) => {
                        if (response && response.status === 200) {
                            const responseClone = response.clone();
                            caches.open(CACHE_NAME).then((cache) => {
                                cache.put(event.request, responseClone);
                            });
                        }
                        return response;
                    });
                })
                .catch(() => caches.match('./index.html'))
        );
    }
});
