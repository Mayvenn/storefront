(ns leads.resolve
  (:require #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [leads.header :as header]
            [storefront.components.footer :as footer]
            [storefront.assets :as assets]
            [storefront.components.svg :as svg]))

(defn social-link [url image-path]
  [:a {:item-prop "sameAs"
       :href url}
   [:img {:src (assets/path image-path)}]])

(defn ^:private component [data owner opts]
  (component/create
   [:div
    (header/built-component data nil)
    [:div.bg-teal.white
     [:div.max-580.center.mx-auto
      [:div.h4.py3.px4
       [:p.m2 "You've just taken the first step towards becoming a Mayvenn!"]
       [:p.p3 "We'll be giving you a call very soon."
        [:br.hide-on-mb]
        " Watch for our Caller ID: 1-510-250-2320"]]
      [:div.h4.py1
       [:p.my2 "We'd love to connect on social media:"]
       [:div
        (social-link "https://www.facebook.com/MayvennHair" "/images/leads/facebook-icon.png")
        (social-link "http://instagram.com/mayvennhair" "/images/leads/instagram-icon-white.png")
        (social-link "http://www.pinterest.com/mayvennhair/" "/images/leads/pinterest-icon.png")
        (social-link "https://twitter.com/mayvennhair" "/images/leads/twitter-icon.png")]]
      [:div.col-10.mx-auto.py8
       [:div.col-12.relative
        {:style {:height "0"
                 :padding-bottom "51%"}}
        [:iframe.col-12.absolute.left-0.top-0
         {:style {:height   "100%"}
          :src   "https://www.youtube.com/embed/MjhjIB2s1Uk"
          :frameBorder 0
          :allowFullscreen true}]]]]]
    (component/build footer/minimal-component (:footer data) nil)]))

(defn ^:private query [data]
  (let [call-number "1-866-424-7201"
        text-number "1-510-447-1504"]
    {:footer {:call-number call-number}
     :faq    {:text-number text-number
              :call-number call-number}}))

(defn built-component [data opts]
  (component/build component (query data) opts))
