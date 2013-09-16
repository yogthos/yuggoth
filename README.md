# Yuggoth

<img src="https://raw.github.com/yogthos/yuggoth/master/logo.png" 
 style="margin-left:50px;"
 hspace="20"
 alt="Yuggoth logo" title="a strange dark orb" align="right" width="200" height="200"/>
 
>"Yuggoth... is a strange dark orb at the very rim of our solar system... 
>There are mighty cities on Yuggoth-great tiers of terraced towers built of black stone... 
>The sun shines there no brighter than a star, but the beings need no light. 
>They have other subtler senses, and put no windows in their great houses and temples..."

> - H. P. Lovecraft, &quot;The Whisperer in Darkness&quot;


Yuggoth is a blog engine which powers my site at http://yogthos.net   

## Features

* content caching
* RSS feed
* tags
* markdown in posts and comments with preview
* syntax highlighting using [syntax highlighter](http://alexgorbatchev.com/SyntaxHighlighter/)
* file uploads and management through web UI
* captchas for comments
* latest comments view
* toggle post visibility
* localization

## Usage

The blog requires an instance of postgreSQL. On the first run the blog will guide you through setting up
the db connection properties and create the necessary tables for you. Then you will be presented with the 
setup wizard that will allow you to configure the administrator and the blog title. Further configuration 
can be done on the profile page.   

### Building and deploying using Leiningen

```bash
lein ring server
```

to run as standalone
```bash
lein ring uberjar
java -jar yuggoth-0.5.0-SNAPSHOT-standalone.jar
```
 
setting the `$PORT` environment variable allows changing the port which the standalone instance runs on


to make a deployable WAR
```bash
lein ring uberwar
```

see [markdown-clj](https://github.com/yogthos/markdown-clj) for supported syntax in posts, any valid HTML will work as well

 
## License

Distributed under the Eclipse Public License, the same as Clojure.

***
Copyright (C) 2012 Yogthos

