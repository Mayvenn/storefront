(ns storefront.components.share-your-cart
  (:require [cemerick.url :as url]
            [storefront.assets :as assets]
            [storefront.component :as component :refer [defcomponent]]
            [storefront.components.share-links :as share-links]
            [storefront.components.popup :as popup]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn facebook-link
  [share-url]
  (share-links/facebook-link (share-links/with-utm-medium share-url "facebook")))

(defn sms-link
  [share-url]
  (share-links/sms-link (str "Shop the bundles I picked for you here: "
                             (share-links/with-utm-medium share-url "sms"))))

(defn twitter-link
  [share-url]
  (share-links/twitter-link (share-links/with-utm-medium share-url "twitter")
                            "Shop my top virgin hair bundle picks here:"))

(defn email-link
  [share-url store-nickname]
  (share-links/email-link "My recommended bundles for you"
                          (str "Hey,

I've created a ready-to-shop cart with the bundles I recommend for you. Mayvenn is my pick for quality virgin human hair. They offer a totally free 30 day exchange program (if you have any issues with your hair at all). All you have to do is click the link below to check out.

Shop here: " (share-links/with-utm-medium share-url "email") "

Let me know if you have any questions.

Thanks,
"
                               store-nickname)))

(defmethod popup/component :share-cart
  [{:keys [share-url utm-url store-nickname]} _ {:keys [close-attrs]}]
  (component/create
   (ui/modal {:close-attrs close-attrs}
             [:div.bg-white.rounded.p4.center
              (ui/modal-close {:close-attrs close-attrs :data-test "share-url-close"})
              [:div.p1
               [:div.h3.navy.medium
                "Share your bag"]
               [:div.h5.dark-gray.light.my2
                "Share this link so your customers know exactly what to buy"]
               [:div.border-top.border-bottom.border-gray.py2
                [:div.clearfix.mxn1
                 [:div.p1.col.col-6.col-12-on-dt
                  [:a.h6.col-12.btn.btn-primary.bg-fb-blue
                   {:href   (facebook-link utm-url)
                    :target "_blank"}
                   (ui/ucare-img {:class "align-middle mr1"
                                  :style {:height "18px"}} "975698f3-3eda-411c-83ad-6a2750e0e59d")
                   "Share"]]
                 [:div.p1.col.col-6.col-12-on-dt
                  [:a.h6.col-12.btn.btn-primary.bg-twitter-blue
                   {:href   (twitter-link utm-url)
                    :target "_blank"}
                   (ui/ucare-img {:class "align-middle mr1"
                                  :style {:height "18px"}} "287fcd79-c45d-453d-94e2-48f0ef6fc35c")
                   "Tweet"]]
                 [:div.p1.col.col-6.col-12-on-dt.hide-on-dt
                  [:a.h6.col-12.btn.btn-primary.bg-sms-green
                   {:href (sms-link utm-url)
                    :target "_blank"}
                   (ui/ucare-img {:class "align-middle mr1"
                                  :style {:height "18px"}} "7e992996-0495-4707-998b-f04ea56c7c73")
                   "SMS"]]
                 [:div.p1.col.col-6.col-12-on-dt
                  [:a.h6.col-12.btn.btn-primary.bg-dark-gray
                   {:href (email-link utm-url store-nickname)
                    :target "_blank"}
                   ^:inline (svg/mail-envelope {:class "stroke-white align-middle mr1"
                                                :style {:height "18px" :width "18px"}})
                   "Email"]]]]
               [:div.mt3.mb1
                [:input.border.border-dark-gray.rounded.pl1.py1.bg-white.teal.col-12
                 {:type      "text"
                  :value     share-url
                  :data-test "share-url"
                  :on-click  utils/select-all-text}]]
               [:div.navy "(select and copy link to share)"]]])))

(defmethod popup/query :share-cart
  [data]
  (let [share-url (get-in data keypaths/shared-cart-url)]
    {:share-url      share-url
     :utm-url        (-> (url/url share-url)
                         (assoc-in [:query :utm_campaign] "sharebuttons"))
     :store-nickname (get-in data keypaths/store-nickname)}))
