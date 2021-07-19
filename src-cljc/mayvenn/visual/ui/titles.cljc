(ns mayvenn.visual.ui.titles
  "
  Defined design elements around titles.

  What's a title? Right now:

  Any text/content box with the following data:
  {id icon primary secondary tertiary}
  "
  (:require [storefront.component :as c]
            [mayvenn.visual.tools :refer [with]]
            [clojure.string :as string]
            [spice.maps :as maps]
            [spice.core :as spice]

            [storefront.components.svg :as svg]))




(def styles
  "These define various classes to assign to parts of a title.


  In the inner maps, the keyword namespace denotes what field to apply the style to,
  while the keyword name is primarily for preventing more than one attribute of a type from
  being applied.
  "
  {:align/center        {:align/class "center"}
   :align/left          {:align/class "left-align"}
   :align/right         {:align/class "right-align"}
   :icon/myj1           {:icon/padding "myj1"}
   :primary/title-1     {:primary/size "title-1"}
   :primary/title-2     {:primary/size "title-2"}
   :primary/title-3     {:primary/size "title-3"}
   :primary/content-1   {:primary/size "content-1"}
   :primary/content-2   {:primary/size "content-2"}
   :primary/content-3   {:primary/size "content-3"}
   :primary/proxima     {:primary/font "proxima"}
   :primary/canela      {:primary/font "canela"}
   :primary/shout       {:primary/emphasis "shout"}
   :primary/flex        {:primary/flex "flex"}
   :primary/flex-auto   {:primary/flex-args "flex-auto"}
   :primary/myj1        {:primary/padding "myj1"}
   :secondary/proxima   {:secondary/font "proxima"}
   :secondary/content-2 {:secondary/size "content-2"}
   :secondary/content-3 {:secondary/size "content-3"}
   :secondary/content-4 {:secondary/size "content-4"}
   :secondary/mt2       {:secondary/padding "mt2"}
   :secondary/myj1      {:secondary/padding "myj1"}
   :tertiary/content-3  {:tertiary/size "content-3"}})


(defn styling<
  "Given a collection of `style` keywords (see styles above)
  merge the corresponding maps for those styles together into a `styling`,


  example:
     (styling< [:align/center
                :primary/title-2
                :primary/shout])

     => {:align/class \"center\"
         :primary/size \"title-2\"
         :primary/emphasis \"shout\"}"
  [style-kws]
  (let [style-maps (map styles style-kws)
        style-keys (mapcat keys style-maps)]
    (assert (= (count (set style-keys)) (count style-keys))
            (str "styling< was called with a keyword collision, "
                 (str style-kws)))
    (apply merge style-maps)))

(defn class<
  "Takes a keyword and a `styling` (see styling< above) and produces a class string
  to be used on a hiccup attribute map

  example:
  (class< :primary {:align/class \"center\"
                    :primary/size \"title-2\"
                    :primary/emphasis \"shout\"})

  => \"title-2 shout\"


  "
  [kw styling]
  (string/join " " (map second (with kw styling))))

(defn should-interpose? [value]
  (and (vector? value)
       (not (-> value first keyword?))))

(defn build-if-value [tag attr value]
  (cond
    (not value) nil
    (should-interpose? value) [tag attr (interpose [:br] value)]
    :else [tag attr value]))

(defn title [style-kws {:keys [id icon primary secondary tertiary]}]
  (let [styling (styling< style-kws)]
    (c/html
     [:div {:class (class< :align styling)}
      [:div {:class (class< :icon styling)}
       (when icon
         (svg/symbolic->html icon))
       (build-if-value :div
               (merge {:class (class< :primary styling)}
                      (when id
                        {:data-test id}))
               primary)]
      (build-if-value :div {:class (class< :secondary styling)}
              secondary)
      (build-if-value :div {:class (class< :tertiary styling)}
              tertiary)])))



(defn
  ^{:usages [:call-out-boxs]}
  proxima [text]
  (title [:align/center
          :primary/title-2
          :primary/proxima
          :primary/shout
          :secondary/content-2
          :secondary/mt2]
         text))

(defn
  ^{:usages [:appointment-booking]}
  proxima-light [text]
  (title [:align/center
          :primary/title-2
          :primary/proxima
          :secondary/content-2
          :secondary/mt2]
         text))

(defn
  ^{:usages [:call-out-boxes]}
  proxima-large [text]
  (title [:align/center
          :primary/title-1
          :primary/proxima
          :primary/shout
          :secondary/mt2
          :secondary/content-1]
         text))

(defn
  ^{:usages [:call-out-boxs]}
  proxima-small [text]
  (title [:align/center
          :primary/title-3
          :primary/proxima
          :primary/shout
          :secondary/content-2
          :secondary/mt2]
         text))

(defn
  ^{:usages [:stylist-cards :pdp/product-summary]}
  proxima-left [text]
  (title [:align/left
          :primary/title-2
          :primary/proxima
          :primary/shout
          :secondary/content-2
          :secondary/mt2]
         text))
(defn
  ^{:usages [:cart-item-card]}
  proxima-tiny [text]
  (title [:align/center
          :primary/content-3
          :primary/proxima
          :secondary/proxima
          :secondary/content-4]
         text))

(defn
  ^{:usages [:cart-item-card]}
  proxima-tiny-right [text]
  (title [:align/right
          :primary/content-2
          :primary/proxima
          :secondary/proxima
          :secondary/content-4]
         text))

(defn
  ^{:usages [:cart-item-card]}
  proxima-small-left
  "Different from normal titles in that the primary styling
  encompasses the icon as well."
  [{:keys [id icon primary secondary]}]
  (c/html
   [:div.left-align
    [:div.proxima.title-3.flex.flex-auto
     (when icon
       (svg/symbolic->html icon))
     [:div
      (when id
        {:data-test id})
      primary]]
    (when secondary
      [:div.mt2.content-2
       secondary])]))

(defn
  ^{:usages [:appointment-booking :lib.ui-cards]}
  proxima-content
  [text]
  (title [:primary/proxima
          :primary/content-2
          :secondary/content-3
          :tertiary/content-3]

         text))

(defn
  ^{:usages [:top-stylist :stylist-search :unified-freeinstall]}
  canela [text]
  (title [:align/center
          :primary/title-2
          :primary/canela
          :secondary/mt2
          :secondary/content-3]
         text))

(defn
  ^{:usages [:top-stylist :stylist-search :unified-freeinstall]}
  canela-left [text]
  (title [:align/left
          :primary/title-2
          :primary/canela
          :secondary/mt2
          :secondary/content-3]
         text))

(defn
  ^{:usages [:call-out-boxes]}
  canela-huge [text]
  (title [:align/center
          :icon/myj1
          :primary/title-1
          :primary/canela
          :primary/myj1
          :secondary/content-2
          :secondary/myj1]
         text))
