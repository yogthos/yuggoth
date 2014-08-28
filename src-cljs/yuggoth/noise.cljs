;;inspired by http://www.lucaongaro.eu/demos/noisegen/
(ns yuggoth.noise)

(def distributions
  {:uniform 1
   :triangular 2
   :bell 5})

(defn random-parametric [n bias]
  (Math/pow
   (/ (->> #(Math/random)
           (repeatedly n)
           (reduce +))
      (dec n))
   (Math/pow 1.2 bias)))

(defn hex->rgb [hex]
  [(bit-shift-right (bit-and hex 0xff0000) 16)
   (bit-shift-right (bit-and hex 0x00ff00) 8)
   (bit-and hex 0x0000ff)])

(defn to-range [n [start end]]
  (Math/floor (+ (* n (- end start)) start)))

(defn rand-rgb [channels? bias distribution from-color to-color]
  (let [rnd (random-parametric distribution bias)]
    (map (partial to-range (if channels? (random-parametric distribution bias) rnd))
      (map vector (hex->rgb from-color) (hex->rgb to-color)))))

(defn make-noise [& {:keys[width height opacity from-color to-color distribution bias grain channels?]
                     :or {width 50
                          height 50
                          opacity 0.2
                          from-color 0x000000
                          to-color 0x606060
                          distribution 5
                          bias 0
                          channels? false
                          grain {:width 1 :height 1}}}]
  (let [canvas (.createElement js/document "canvas")
        context (.getContext canvas "2d")
        distribution (or (distributions distribution) distribution)]
    (set! (.-width canvas) width)
    (set! (.-height canvas) height)
    (loop [x width]
      (loop [y height]
        (let [[r g b] (rand-rgb channels? bias distribution from-color to-color)]
          (set! (.-fillStyle context) (str "rgba(" r "," g "," b "," opacity ")"))
          (.fillRect context x y (:width grain) (:height grain)))

        (when-not (neg? y)
        (recur (- y (:height grain)))))
      (when-not (neg? x)
        (recur (- x (:width grain)))))
    (.toDataURL canvas "image/png")))

(defn set-background [target img]
  (set! (-> target .-style .-backgroundImage) (str "url('" img "')")))

(defn set-body-background [img]
   (set-background (.-body js/document) img))

(defn set-element-background [id img]
  (set-background (.getElementById js/document id) img))
