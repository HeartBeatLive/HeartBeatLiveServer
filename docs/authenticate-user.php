<?php

if (!isset($_SERVER['PHP_AUTH_USER'])) {
    header('WWW-Authenticate: Basic realm="HeartBeatApp docs"');
    header('HTTP/1.0 401 Unauthorized');
    echo 'Please, authenticate first.';
    exit;
}

if ($_SERVER['PHP_AUTH_USER'] != $_ENV['DOCS_USERNAME'] || $_SERVER['PHP_AUTH_PW'] != $_ENV['DOCS_PASSWORD']) {
    echo 'Username or password is incorrect!';
    exit;
}

?>