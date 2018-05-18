(ns leads.resolve
  (:require [storefront.component :as component]
            #?@(:cljs [[storefront.browser.cookie-jar :as cookie-jar]])
            [leads.header :as header]
            [leads.keypaths :as keypaths]
            [leads.flows :as flows]
            [storefront.assets :as assets]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.config :as config]
            [storefront.keypaths :as storefront.keypaths]
            [storefront.components.ui :as ui]
            [storefront.components.footer-minimal :as footer-minimal]))

(defn social-link [url image-path]
  [:a {:item-prop "sameAs"
       :href url}
   [:img {:src (assets/path image-path)}]])

(defn ^:private component [queried-data owner opts]
  (component/create
   [:div
    (component/build header/component (:header queried-data) nil)
    [:div.bg-teal.white
     [:div.max-580.center.mx-auto
      [:div.h4.py3.px4
       [:p.m2 "You've just taken the first step towards becoming a Mayvenn!"]
       [:p.p3 "We'll be giving you a call very soon."
        [:br.hide-on-mb]
        " Watch for our Caller ID: 510-694-0253"]]
      [:div.h4.py1
       [:p.my2 "We'd love to connect on social media:"]
       [:div
        (social-link "https://www.facebook.com/MayvennHair" "//ucarecdn.com/5d322fe0-d0cb-4d62-a014-342b46fad2c1/-/format/auto/facebookicon.png")
        (social-link "http://instagram.com/mayvennhair" "//ucarecdn.com/05a11abb-b2e3-4b89-a43a-68d40f71242d/-/format/auto/instagramiconwhite.png")
        (social-link "http://www.pinterest.com/mayvennhair/" "//ucarecdn.com/dc77e98c-1fda-4267-aba2-dce4b1dd0ecc/-/format/auto/pinteresticon.png")
        (social-link "https://twitter.com/mayvennhair" "//ucarecdn.com/41683ed1-1494-4c44-a3b0-41a25eab744e/-/format/auto/twittericon.png")]]
      [:div.col-10.mx-auto.py8
       (ui/youtube-responsive "https://www.youtube.com/embed/MjhjIB2s1Uk")]]]
    (component/build footer-minimal/component (:footer queried-data) nil)]))

(defn ^:private query [data]
  {:header (header/query data)
   :footer (footer-minimal/query data)})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-leads-resolve
  [_ _ _ _ app-state]
  #?(:cljs
     (cookie-jar/save-lead (get-in app-state storefront.keypaths/cookie)
                           {"lead-id" (get-in app-state keypaths/remote-lead-id)})))

