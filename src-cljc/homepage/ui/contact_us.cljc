(ns homepage.ui.contact-us
  (:require [storefront.component :as c]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

(defn ^:private contact-us-contact-method-molecule
  [{:contact-us.contact-method/keys [uri title copy svg-symbol legal-copy legal-links]} id]
  (let [legal? (boolean (or legal-copy legal-links))]
    [:div.col-12.col-4-on-tb-dt
     {:class (if legal? "mb3" "")
      :key   id}
     [:a.pt3.block.black
      {:href  uri
       :class (if legal? "" "pb3")}
      [:div.mt6-on-dt.mb4-on-dt (svg/symbolic->html svg-symbol)]
      [:div.proxima.title-2.mt1
       title]
      [:div.col-8.mx-auto.p-color.content-2
       copy]]
     (when legal?
       [:div.col-8.mx-auto.p-color.content-3.pt1.black
        (str legal-copy)
        (when legal-links
          [:span
           [:br]
           "See "
           (interpose " & " (for [{:keys [copy target]} legal-links]
                              [:a (merge {:key copy} (apply utils/route-to target)) (str copy)]))
           "."])]) ]))

(defn ^:private contact-us-title-molecule
  [{:contact-us.title/keys [primary secondary]}]
  [:div
   [:h2.mt6.proxima.shout.title-2 primary]
   [:div.canela.title-1.pb1 secondary]])

(defn ^:private contact-us-body-molecule
  [{:contact-us.body/keys [primary]}]
  [:div.proxima.content-2 primary])

(def contact-us-straight-line-atom
  [:div.stroke-s-color.pt4.hide-on-dt
   (svg/straight-line {:width  "1px"
                       :height "42px"})])

(defn ^:private contact-methods-list-molecule
  [{:list/keys [contact-methods]}]
  (for [[idx element] (map-indexed vector contact-methods)
        :let [id (str "contact-us.contact-method" idx)]]
    (contact-us-contact-method-molecule element id)))

(c/defcomponent organism
  [data _ _]
  [:div
   [:div.bg-warm-gray.center.py8
    (contact-us-title-molecule data)
    (contact-us-body-molecule data)
    contact-us-straight-line-atom
    [:div.flex.flex-wrap.items-baseline.justify-center.col-12.col-8-on-tb-dt.mx-auto.pt3-on-dt
     (contact-methods-list-molecule data)]]])
