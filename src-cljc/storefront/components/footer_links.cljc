(ns storefront.components.footer-links
  (:require #?@(:cljs [[storefront.browser.scroll :as scroll]])
            [spice.date :as date]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.routes :as routes]
            [storefront.trackings :as trackings]
            [storefront.transitions :as transitions]))

(defn- footer-link [opts label]
  (component/html [:a.block.inherit-color.my2 opts label]))

(defn- minimal-footer-link [opts label]
  (component/html [:a.inherit-color ^:attrs opts ^:inline label]))

(defn signup-molecule
  [{:footer.email-signup.title/keys     [id primary]
    :footer.email-signup.submitted/keys [text]
    :footer.email-signup.input/keys     [value errors]
    :footer.email-signup.button/keys    [label placeholder target focus-target] :as data} view]
  (when id
    (component/html
     [:div.mb5.dark-gray
      [:div.content-2 primary]
      [:form
       {:on-submit (apply utils/send-event-callback target)}
       [:div.py2.dark-gray
        (ui/input-group
         {:class         "dark-gray"
          :type          "email"
          :required      true
          :wrapper-class "flex-grow-1 bg-gray-mask border-none"
          :data-test     (str "email-address-" view)
          :on-focus      (apply utils/send-event-callback focus-target)
          :focused       true
          :placeholder   placeholder
          :value         value
          :errors        (get errors ["email"])
          :data-ref      (str id "-" view)}
         {:content label
          :args    {:class     "bg-p-color border-none"
                    :on-click  (apply utils/send-event-callback target)
                    :spinning? nil
                    :data-test (str "sign-up-" view)}})]]
      (when text
        [:div.mb7.mtn2 text])])))

(defcomponent component [{:keys [minimal?] :as data} owner opts]
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
     [:div.p3.container
      [:div.flex.justify-between
       [:div.col-6
        ^:inline (svg/mayvenn-text-logo {:height "29px"
                                        :width  "115px"
                                         :class  "fill-white"})
        [:div.flex.mt4.mb3.col-12-on-dt
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
       [:div.px3.col-6-on-tb-dt.hide-on-mb
        (signup-molecule data "on-tb-dt")]]]
     [:div.px3.hide-on-tb-dt
      (signup-molecule data "on-mb")]
     [:div.bg-gray-mask
      [:div.px3.container
       [:span.py2.flex.items-center.gray {:key "minimal"}
        "©" (date/year (date/now)) " " "Mayvenn"]]]]))

(defn query
  [{:keys [minimal-footer? footer-email-signup? footer-email-submitted? footer-field-errors footer-ready-for-email-signup?]}]
  (merge
   {:minimal?     minimal-footer?
    :email        nil
    :field-errors nil}
   {:footer.email-signup.title/id            (when footer-email-signup? "sign-up")
    :footer.email-signup.title/primary       "Sign up to get the latest on sales, new releases and more..."
    :footer.email-signup.button/label        (if (or footer-ready-for-email-signup?
                                                     (not footer-email-submitted?))
                                              "sign up"
                                              (svg/check-mark {:class "fill-white mx3"
                                                               :style {:height "18px" :width "18px"}}))
    :footer.email-signup.button/target       [events/control-footer-email-submit]
    :footer.email-signup.button/focus-target [events/control-footer-email-on-focus]
    :footer.email-signup.button/placeholder  "Email address"
    :footer.email-signup.input/errors        footer-field-errors
    :footer.email-signup.submitted/text      (when footer-email-submitted? "You have successfully subscribed to our mailing list.")}))

(defn built-component
  [data opts]
  (component/build component (query data) nil))

(defmethod transitions/transition-state events/control-footer-email-submit
  [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/footer-email-ready false)
      (assoc-in keypaths/footer-email-submitted true)))

(defmethod transitions/transition-state events/control-footer-email-on-focus
  [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/footer-email-ready true)))

(defmethod effects/perform-effects events/control-footer-email-submit
  [_ event args app-state]
  #?(:cljs (scroll/scroll-selector-to-top "[data-ref=sign-up-on-mb]")
      :clj nil))
