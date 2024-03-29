(ns storefront.components.footer-links
  (:require #?@(:cljs [[storefront.browser.scroll :as scroll]
                       [storefront.hooks.stringer :as stringer]])
            [clojure.string :as string]
            [spice.date :as date]
            [storefront.accessors.nav :as nav]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.events :as events]
            [storefront.effects :as effects]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]
            [storefront.platform.messages :as messages]
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
    :footer.email-signup.button/keys    [label placeholder target focus-target disabled?]} view]
  (when id
    (component/html
     [:div.mb5.gray
      [:div.content-2 primary]
      [:form
       {:on-submit (apply utils/send-event-callback target)}
       [:div.py2.dark-gray
        (ui/input-group
         {:class         "dark-gray"
          :keypath       keypaths/footer-email-value
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
          :args    {:class          "bg-p-color border-none"
                    :disabled?      disabled?
                    :on-click       (apply utils/send-event-callback target)
                    :data-test      (str "sign-up-" view)
                    :type           "submit"}})]]
      (when text
        [:div.mb7.mtn2 text])])))

(def ^:private dns-url "https://mayvenn.wirewheel.io/privacy-page/5f22b71054ee7d0012420211")

(defcomponent component [{:keys [minimal? footer-email-capture?] :as data} owner opts]
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
     ^:inline (minimal-footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")
     " - "
     ^:inline (minimal-footer-link (assoc (utils/route-to events/navigate-content-about-us)
                                          :data-test "content-about-us") "Return & Exchange Policy")]
    [:div.flex-column.content-3.white.bg-black.pt6
     [:div.p3.container
      [:div.flex.justify-between
       [:div.col-12.col-6-on-dt
        ^:inline (svg/mayvenn-text-logo {:height "29px"
                                         :width  "115px"
                                         :class  "fill-white"})
        [:div.flex.mt4.mb3.col-12-on-dt
         [:div.col-3 {:key "full"}
          ^:inline (footer-link (merge {:aria-label "About Mayvenn"} (utils/route-to events/navigate-content-about-us)) "About")
          ^:inline (footer-link {:href "https://jobs.mayvenn.com"} "Careers")
        ^:inline (footer-link (utils/route-to events/navigate-content-help) "Contact")]
         [:div.col-3 {:key "standard"}
        ^:inline (footer-link (assoc (utils/route-to events/navigate-content-privacy)
                                     :data-test "content-privacy") "Privacy")
        ^:inline (footer-link (assoc (utils/route-to events/navigate-content-tos)
                                     :data-test "content-tos") "Terms")

        ;; use traditional page load so anchors work
          ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#our-ads")} "Our Ads")]
       [:div.col-6
        ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-guarantee) "#guarantee")}
                              "Our Guarantee")
        ;; use traditional page load so anchors work
        ^:inline (footer-link {:href (str (routes/path-for events/navigate-content-privacy) "#ca-privacy-rights")}
                              "CA Privacy Rights")
        ^:inline (footer-link {:href dns-url} "Do Not Sell My Personal Information")]]]
       (when footer-email-capture?
         [:div.px3.col-6-on-tb-dt.hide-on-mb
          (signup-molecule data "on-tb-dt")])]]
     (when footer-email-capture?
       [:div.px3.hide-on-tb-dt
        (signup-molecule data "on-mb")])
     [:div
      [:div.px3.container
       [:span.py2.flex.items-center.white {:key "minimal"}
        "©" (date/year (date/now)) " " "Mayvenn"]]]]))

(defn ^:private invalid-email? [email]
  (not (and (seq email)
            (< 3 (count email))
            (string/includes? email "@")
            (not (string/ends-with? email "@")))))

(defn query
  [{:keys [minimal-footer? footer-email-input-value
           footer-email-submitted? footer-field-errors footer-ready-for-email-signup?
           footer-email-capture?]}]
  (merge
   {:minimal?     minimal-footer?
    :email        nil
    :field-errors nil}
   {:footer.email-signup.title/id            "sign-up"
    :footer.email-signup.title/primary       "Sign up to get the latest on sales, new releases and more..."
    :footer.email-signup.button/label        (if (or footer-ready-for-email-signup?
                                                     (not footer-email-submitted?))
                                              "sign up"
                                              (svg/check-mark {:class "fill-white mx3"
                                                               :style {:height "18px" :width "18px"}}))
    :footer.email-signup.button/target       [events/control-footer-email-submit {:email footer-email-input-value}]
    :footer.email-signup.button/focus-target [events/control-footer-email-on-focus]
    :footer.email-signup.button/placeholder  "Email address"
    :footer.email-signup.button/disabled?    (and footer-email-input-value
                                                  (invalid-email? footer-email-input-value))
    :footer.email-signup.input/errors        footer-field-errors
    :footer.email-signup.input/value         footer-email-input-value
    :footer.email-signup.submitted/text      (when footer-email-submitted? "You have successfully subscribed to our mailing list.")
    :footer-email-capture?                   footer-email-capture?}))

(defn built-component
  [data opts]
  (component/build component (query data) nil))

(defmethod transitions/transition-state events/control-footer-email-on-focus
  [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/footer-email-ready true)
      (assoc-in keypaths/footer-email-value nil)))

#?(:cljs
   (defmethod trackings/perform-track events/control-footer-email-submit
     [_ event {:keys [email]} app-state]
     (stringer/track-event "footer_email_capture"
                           {:email            email
                            :store-slug       (get-in app-state keypaths/store-slug)
                            :test-variations  (get-in app-state keypaths/features)
                            :store-experience (get-in app-state keypaths/store-experience)})))

(defmethod transitions/transition-state events/control-footer-email-submit
  [_ event args app-state]
  (-> app-state
      (assoc-in keypaths/footer-email-ready false)
      (assoc-in keypaths/footer-email-submitted true)))

(defmethod effects/perform-effects events/control-footer-email-submit
  [_ event {:keys [email]} app-state]
  #?(:cljs (do
             (messages/handle-message events/user-identified)
             (scroll/scroll-selector-to-top "[data-ref=sign-up-on-mb]"))
      :clj nil))
