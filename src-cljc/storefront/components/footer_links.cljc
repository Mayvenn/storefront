(ns storefront.components.footer-links
  (:require [spice.date :as date]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]))

(defn- footer-link [opts label]
  (component/html [:a.block.inherit-color.my2 opts label]))

(defn- minimal-footer-link [opts label]
  (component/html [:a.inherit-color ^:attrs opts ^:inline label]))

(defcomponent component [{:keys [minimal? field-errors email]} owner opts]
  (if minimal?
    [:div.content-3.proxima.center
     ^:inline (minimal-footer-link (assoc (utils/route-to events/navigate-content-privacy)
                                          :data-test "content-privacy") "Privacy")
     " - "
     ^:inline (minimal-footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
                                   "CA Privacy Rights")
     " - "
     ^:inline (minimal-footer-link (assoc (utils/route-to events/navigate-content-tos)
                                          :data-test "content-tos") "Terms")
     " - "
     ;; use traditional page load so anchors work
     ^:inline (minimal-footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")]
    [:div.flex-column.content-3.white.bg-black.pt6
     [:div.px3.container
      [:div.flex.justify-between
       ^:inline (svg/mayvenn-text-logo {:height "29px"
                                        :width  "115px"
                                        :class  "fill-white"})]
      [:div.flex.mt4.mb3.col-5-on-dt
       [:div.col-4 {:key "full"}
        ^:inline (footer-link (utils/route-to events/navigate-content-about-us) "About")
        ^:inline (footer-link {:href "https://jobs.mayvenn.com"} "Careers")
        ^:inline (footer-link (utils/route-to events/navigate-content-help) "Contact")]
       [:div.col-4 {:key "standard"}
        ^:inline (footer-link (assoc (utils/route-to events/navigate-content-privacy)
                                     :data-test "content-privacy") "Privacy")
        ^:inline (footer-link (assoc (utils/route-to events/navigate-content-tos)
                                     :data-test "content-tos") "Terms")

        ;; use traditional page load so anchors work
        ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")]
       [:div.col-4
        ;; use traditional page load so anchors work
        ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
                              "CA Privacy Rights")]]]
     [:div.px3
      [:div.content-2.dark-gray "Sign up to get the latest on sales, new releases and more..."]
      [:form
       {:on-submit nil}
       [:div.py2.dark-gray
        (ui/input-group
         {:keypath       nil
          :class         "dark-gray"
          :wrapper-class "flex-grow-1 bg-gray-mask border-none"
          :data-test     "email-address"
          :focused       true
          :placeholder   "Email address"
          :value         email
          :errors        (get field-errors ["email"])
          :data-ref      "voucher-code"}
         {:content "sign up"
          :args    {:class     "bg-p-color border-none"
                    :on-click  nil
                    :spinning? nil
                    :data-test "sign-up"}})]]]
     [:div.bg-gray-mask
      [:div.px3.container
       [:span.py2.flex.items-center.gray {:key "minimal"}
        "©" (date/year (date/now)) " " "Mayvenn"]]]]))

(defn query
  [data]
  {:minimal?     (nav/show-minimal-footer? (get-in data keypaths/navigation-event))
   :email        nil
   :field-errors nil})

(defn built-component
  [data opts]
  (component/build component (query data) nil))
