const http = require('http');
const fs = require('fs');

const PORT = process.env.PORT || 8080;
const ROOT_CREDENTIALS = process.env.CREDENTIALS || 'username:password';
const FILES_PREFIX = process.env.FILES_PREFIX || "./public/";

const STATIC_FILES = {
    '/javascripts/spectaql.min.js': { contentType: 'text/javascript' },
    '/stylesheets/spectaql.min.css': { contentType: 'text/css' },
    '/schema.graphqls': { contentType: 'text/plain', secure: true }
};

http.createServer((req, resp) => {
    const staticFile = STATIC_FILES[req.url];
    if (staticFile) {
        if (staticFile.secure) {
            const authenticated = secureEndpoint(req, resp);
            if (!authenticated) {
                return;
            }
        }

        resp.setHeader("Content-Type", staticFile.contentType);
        resp.writeHead(200);

        const fileData = loadFile(req.url);
        resp.end(fileData);
    } else if (req.url === '/') {
        const authenticated = secureEndpoint(req, resp);
        if (!authenticated) {
            return;
        }

        resp.setHeader("Content-Type", "text/html");
        resp.writeHead(200);

        const fileData = loadFile('/index.html');
        resp.end(fileData);
    } else {
        resp.setHeader("Content-Type", "text/plain");
        resp.writeHead(404);
        resp.end('Not Found!');
    }
}).listen(PORT);

function secureEndpoint(req, resp) {
    const authorization = req.headers.authorization || '';

    const credentials = Buffer.from(
        authorization.split(' ')[1] || '',
        'base64'
    ).toString();

    if (credentials !== ROOT_CREDENTIALS) {
        resp.writeHead(401, { 'WWW-Authenticate': 'Basic realm="nope"' });
        resp.end('HTTP Error 401 Unauthorized: Access is denied');
        return false;
    }

    return true;
}

function loadFile(path) {
    return fs.readFileSync(FILES_PREFIX + path)
}