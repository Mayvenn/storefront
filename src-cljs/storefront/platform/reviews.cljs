(ns storefront.platform.reviews
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.app-routes :as routes]
            [storefront.keypaths :as keypaths]))

(def product-options-by-named-search
  {:straight       {:data-product-id  80
                    :data-name        "Brazilian Natural Straight Hair"
                    :data-description "Perfect for those who mostly wear straight hair extensions. After washing, our Brazilian Straight hair extensions have just a hint of wave that can be easily flat ironed. Lightweight and silky. Holds a curl beautifully. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Can be easily colored or bleached (consult a licensed cosmetologist)."
                    :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414704/6154340/thumb.jpg"}
   :kinky-straight {:data-product-id  525
                    :data-name        "Brazilian Kinky Straight Hair"
                    :data-description "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee. Our kinky straight hair mimic the blown-out texture of your natural hair."
                    :data-image-url   "https://d275k6vjijb2m1.cloudfront.net/cellar/kinky_straight/1/250x227.jpg"}
   :loose-wave     {:data-product-id  2
                    :data-name        "Brazilian Loose Wave Hair"
                    :data-description "Our Brazilian Loose Wave human hair has been steam processed so you can enjoy long-lasting beautiful waves. The waves are tighter than our body wave texture but looser than our deep wave texture. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Excessive flat-ironing will loosen the curls over time. These hair extensions can be colored (consult a licensed cosmetologist)."
                    :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414703/6154339/thumb.jpg"}
   :deep-wave      {:data-product-id  67
                    :data-name        "Brazilian Deep Wave Hair"
                    :data-description "Premier Brazilian Deep Wave
Material:High Quality 100% Virgin Human Hair
Color: 1B
Bleach-able up to level:7 or 8 (#27)
Expected Longevity: 3 months + (Depending on care)
 Texture & Description:
The Deep Wave texture has a wave that can be easily flat ironed.
In Between textures Curly and Loose Wave.
Each bundle weighs 3.5 ounces.
Available in a natural dark brown color equivalent to color 1B.
These hair extensions can be colored (consult a licensed cosmetologist).
Excessive flat-ironing will loosen the texture over time.

Lengths: 12\" to 28\""
                    :data-image-url "http://s3.amazonaws.com/yotpo-images-production/Product/6414702/6154338/thumb.jpg"}
   :body-wave {:data-product-id  15
               :data-name        "Malaysian Body Wave Hair"
               :data-description "Our Malaysian Body Wave human hair has been steam processed so you can enjoy long-lasting beautiful waves. This texture has an \"S\" shape wave that is soft and bouncy. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Excessive flat-ironing will loosen the curls over time. These hair extensions can be colored or bleached (consult a licensed cosmetologist)."
               :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414706/6318976/thumb.jpg"}
   :curly     {:data-product-id  54
               :data-name        "Brazilian Curly Hair"
               :data-description "Premier Brazilian Curly
Material:High Quality 100% Virgin Human Hair
Color: 1B
Bleach-able up to level:7 or 8 (#27)
Expected Longevity: 3 months + (Depending on care)
Texture & Description:
Premier Brazilian curly human hair can be flat ironed and still return to its natural curl pattern.
When purchasing the curly hair extensions, keep in mind that each bundled is measured when the hair is pulled straight. We recommend going 2 inches longer to get desired length (i.e To get 12 inches of hair, buy a 14 inch bundle instead).
Holds a natural tight curl beautifully.
Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. These hair extensions can be colored (consult a licensed cosmetologist). Excessive flat-ironing will loosen the curls over time.
Lengths: 12\" to 28\""
               :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414708/6154343/thumb.jpg"}
   :closures  {:data-product-id  90
               :data-name        "Brazilian Natural Straight Lace Closure"
               :data-description "Our 4x4 lace closures create an illusion of a scalp with a natural density, and is used to leave more of your own natural hair protected. The lace closure has a natural brown scalp color and can be restyled, re-parted, cut, and colored."
               :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6575214/6319000/thumb.jpg"}
   :frontals  {:data-product-id  90
               :data-name        "Brazilian Natural Straight Lace Closure"
               :data-description "Our 4x4 lace closures create an illusion of a scalp with a natural density, and is used to leave more of your own natural hair protected. The lace closure has a natural brown scalp color and can be restyled, re-parted, cut, and colored."
               :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6575214/6319000/thumb.jpg"}})

(defn product-options-for [{:keys [slug]}]
  (get product-options-by-named-search (keyword slug)))

(defn reviews-component-inner [{:keys [loaded? named-search url]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (handle-message events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (handle-message events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div
        (when loaded?
          [:.mx-auto.mb3
           [:.yotpo.yotpo-main-widget
            (merge
             (product-options-for named-search)
             {:data-url url})]])]))))

(defn reviews-component [{:keys [named-search] :as args} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-" (:slug named-search))}
     (om/build reviews-component-inner args opts)])))

(defn reviews-summary-component-inner [{:keys [loaded? named-search url]} owner opts]
  (reify
    om/IDidMount
    (did-mount [_] (handle-message events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (handle-message events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div
        (when loaded?
          [:.clearfix.flex.justify-center.flex-wrap.my1
           [:.yotpo.bottomLine.mr2
            (merge
             (product-options-for named-search)
             {:data-url url})]
           [:.yotpo.QABottomLine
            (product-options-for named-search)]])]))))

(defn reviews-summary-component [{:keys [named-search] :as args} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-summary-" (:slug named-search))}
     (om/build reviews-summary-component-inner args opts)])))

(defn query [data]
  {:url          (routes/current-path data)
   :named-search (named-searches/current-named-search data)
   :loaded?      (get-in data keypaths/loaded-reviews)})
