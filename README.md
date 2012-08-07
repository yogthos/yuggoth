# Yuggoth

<img src="https://raw.github.com/yogthos/yuggoth/master/logo.jpg" align="right" style="margin-left:50px;"
 alt="Yuggoth logo" title="a strange dark orb" align="right" width="200" height="200"/>
 
>"Yuggoth... is a strange dark orb at the very rim of our solar system... 
>There are mighty cities on Yuggoth—great tiers of terraced towers built of black stone... 
>The sun shines there no brighter than a star, but the beings need no light. 
>They have other subtler senses, and put no windows in their great houses and temples..."

> — H. P. Lovecraft, &quot;The Whisperer in Darkness&quot;


Yuggoth is a blog engine which powers my site at http://yogthos.net and allows me to experiment with using Noir.   

## Features

* fast
* content caching
* RSS feed
* markdown content editing with live preview
* file uploads and management
* custom styles
* captchas for comments
* view latest comments

## Usage

First, setup postgreSQL and update `db` definition in `yuggoth.models.db` to point to it, the blog will automatically create the necessary tables for you. 
When you navigate to the blog on the first run it will present the setup wizard wich will allow you to configure the administrator and the blog title.
Further configuration can be done on the profile page.   

If you use cake, substitute 'lein' with 'cake' below. Everything should work fine.

```bash
lein deps
lein run
```

to enable SSL uncomment `secure-login-redirect` in `service.clj` and optionally change the port to the one you're using, default is 443

to run as standalone
```bash
lein uberjar
java -jar yuggoth-0.4.0-SNAPSHOT-standalone.jar
```

valid options for running standalone

* -mode dev or prod
* -port integer

to make a deployable WAR
```bash
lein ring uberwar
```



see [markdown-clj](https://github.com/yogthos/markdown-clj) for supported syntax in posts, any valid HTML will work as well

 
## License

Distributed under the Eclipse Public License, the same as Clojure.

***
Copyright (C) 2012 Yogthos

