export default {
    async fetch(request, env) {
        const url = new URL(request.url);
        const hostname = url.hostname;

        let targetUrl;

        if (hostname === 'tlab29.com' || hostname === 'www.tlab29.com') {
            targetUrl = new URL(`https://5.147.111.121:8443${url.pathname}${url.search}`);
        } else if (hostname === 'api.tlab29.com') {
            targetUrl = new URL(`https://5.147.111.121:8443${url.pathname}${url.search}`);
        } else {
            return new Response('Not found', { status: 404 });
        }

        // Копируем заголовки из оригинального запроса
        const headers = new Headers();
        for (const [key, value] of request.headers) {
            // Не копируем Host header, так как мы его заменим
            if (key.toLowerCase() !== 'host') {
                headers.set(key, value);
            }
        }

        // Устанавливаем правильный Host header для Traefik
        headers.set('Host', hostname);

        // Выполняем HTTPS запрос к нашему серверу
        try {
            const response = await fetch(targetUrl, {
                method: request.method,
                headers: headers,
                body: request.body,
                redirect: 'manual'
            });

            return new Response(response.body, {
                status: response.status,
                statusText: response.statusText,
                headers: response.headers
            });
        } catch (error) {
            return new Response(`Origin server error: ${error.message}`, {
                status: 502
            });
        }
    }
};