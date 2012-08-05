# Yuggoth

>"Yuggoth... is a strange dark orb at the very rim of our solar system... 
>There are mighty cities on Yuggoth—great tiers of terraced towers built of black stone... 
>The sun shines there no brighter than a star, but the beings need no light. 
>They have other subtler senses, and put no windows in their great houses and temples..."

> — H. P. Lovecraft, "The Whisperer in Darkness"

Yuggoth is a blog engine which powers my site at http://yogthos.net and allows me to experiment with using Noir.   

## Features

* RSS feed
* markdown content editing with live preview
* file uploads and management
* custom styles
* captchas for comments


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

to make a deployable WAR
```bash
lein ring uberwar
```

valid options

* -mode dev or prod
* -port integer


see [markdown-clj](https://github.com/yogthos/markdown-clj) for supported syntax in posts, any valid HTML will work as well

 
## License

Distributed under the Eclipse Public License, the same as Clojure.

***
Copyright (C) 2012 Yogthos

