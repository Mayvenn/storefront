(ns storefront.platform.reviews
  (:require [sablono.core :refer [html]]
            [om.core :as om]
            [storefront.events :as events]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [catalog.products :as products]))

(def product-options-by-named-search
  {:straight       {:data-product-id  80
                    :data-name        "Brazilian Natural Straight Hair"
                    :data-description "Perfect for those who mostly wear straight hair extensions. After washing, our Brazilian Straight hair extensions have just a hint of wave that can be easily flat ironed. Lightweight and silky. Holds a curl beautifully. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Can be easily colored or bleached (consult a licensed cosmetologist)."
                    :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414704/6154340/thumb.jpg"}
   :yaki-straight  {:data-product-id  642
                    :data-name        "Brazilian Yaki Straight Hair"
                    :data-description "Our Yaki Straight hair matches the rhythm of natural hair that's been pressed straight or freshly relaxed. This hair is lightweight, yet full of body and flowy movement. 100% human hair, machine-wefted and backed by our 30 Day Quality Guarantee, it's a great option for those who want a textured straightened look that can be flat ironed to be even more sleek when desired."
                    :data-image-url   "http://ucarecdn.com/98e8b217-73ee-475a-8f5e-2c3aaa56af42/-/scale_crop/250x227/Yaki-Straight-Bundle.jpg"}
   :kinky-straight {:data-product-id  525
                    :data-name        "Brazilian Kinky Straight Hair"
                    :data-description "100% virgin human hair, machine-wefted and backed by our 30 Day Quality Guarantee. Our kinky straight hair mimic the blown-out texture of your natural hair."
                    :data-image-url   "https://d275k6vjijb2m1.cloudfront.net/cellar/kinky_straight/3/250x227_1.jpg"}
   :body-wave      {:data-product-id  15
                    :data-name        "Malaysian Body Wave Hair"
                    :data-description "Our Malaysian Body Wave human hair has been steam processed so you can enjoy long-lasting beautiful waves. This texture has an \"S\" shape wave that is soft and bouncy. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Excessive flat-ironing will loosen the curls over time. These hair extensions can be colored or bleached (consult a licensed cosmetologist)."
                    :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414706/6318976/thumb.jpg"}
   :loose-wave     {:data-product-id  2
                    :data-name        "Brazilian Loose Wave Hair"
                    :data-description "Our Brazilian Loose Wave human hair has been steam processed so you can enjoy long-lasting beautiful waves. The waves are tighter than our body wave texture but looser than our deep wave texture. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Excessive flat-ironing will loosen the curls over time. These hair extensions can be colored (consult a licensed cosmetologist)."
                    :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414703/6154339/thumb.jpg"}
   :water-wave     {:data-product-id  618
                    :data-name        "Brazilian Water Wave Hair"
                    :data-description "No beach necessary with our luscious Water Wave hair. This texture is your tropical getaway in the form of loose, carefree curly bundles. With it's soft S-shaped and C-shaped coils, it's versatility and styling options are endless. 100% human hair, machine-wefted and backed by our 30 Day Quality Guarantee, bask in your glow by wearing it wet & sleek or voluminous & fluffy."
                    :data-image-url   "http://ucarecdn.com/5f6c669f-8274-4bef-afa9-3c08813842f6/-/scale_crop/250x227/Water-Wave-Bundle.jpg"}
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
   :curly        {:data-product-id  54
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
   :closures     {:data-product-id  601
                  :data-name        "Brazilian Natural Straight Lace Closure"
                  :data-description "Our 4x4 lace closures create an illusion of a scalp with a natural density, and is used to leave more of your own natural hair protected. The lace closure has a natural brown scalp color and can be restyled, re-parted, cut, and colored."
                  :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6575214/6319000/thumb.jpg"}
   :frontals     {:data-product-id  601
                  :data-name        "Brazilian Natural Straight Lace Closure"
                  :data-description "Our 4x4 lace closures create an illusion of a scalp with a natural density, and is used to leave more of your own natural hair protected. The lace closure has a natural brown scalp color and can be restyled, re-parted, cut, and colored."
                  :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6575214/6319000/thumb.jpg"}
   :360-frontals {:data-product-id  668
                  :data-name        "Brazilian Natural Straight 360 Frontal Lace Closure"
                  :data-description "From your hairline to nape, we’ve got you covered with our revolutionary 360 Lace Frontal. This one-of-a-kind frontal piece features freestyle parting, baby hairs, and low-density edges for a naturally flawless look. Measuring 4” in both the front and sides, and 2.5” in the back, our 360 Lace Frontal allows plenty of space for all your customization needs. Throw it in a high bun, rock a pair of french braids, or don a ponytail - the 360 Lace Frontal provides the flexibility for all-around hairstyles that look great from all sides."
                  :data-image-url   "https://ucarecdn.com/7837332a-2ca5-40dd-aa0e-86a2417cd723/-/scale_crop/250x227/Straight-360-Frontal-From-Three-Quarters-Back.jpg"}
   :wigs         {:data-product-id  698
                  :data-name        "Brazilian Natural Straight 360 Wig"
                  :data-description "Designed for those who want all-around protection, our 360 Lace Frontal Wig in Straight has you covered from front to back. No more waiting hours to get an install - our 360 Straight unit provides the versatility of style that can be achieved in an instant! Our wigs are made from 100% virgin human hair and can be customized to fit your unique look using the built-in clips and adjustable strap. They feature natural density throughout and can be parted from ear-to-ear or worn up in a ponytail with our 360 lace frontal construction."
                  :data-image-url   "http://ucarecdn.com/10b9786e-846e-4e74-a6ac-d7b3b89beaed/-/scale_crop/250x227/Straight-360-Wig.jpg"}
   :dyed         {:data-product-id  90
                  :data-name        "Dyed Virgin Indian Straight Lace Frontal"
                  :data-description "100% human hair, hand-tied and backed by our 30 Day Quality Guarantee, our 13\" x 4\" lace frontals mimic a natural hairline and offer versatile parting options as well as small hairline hairs that can be tweezed to achieve your desired look. "
                  :data-image-url   "http://ucarecdn.com/c9b721df-802a-4cd6-990e-2b81d20e7aa5/-/scale_crop/250x227/yotpo_dyed_frontal"}})

(defn product-options-for [slug]
  (get product-options-by-named-search (keyword slug)))

(defn reviews-component-inner [{:keys [loaded? named-search-slug url]} owner opts]
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
             (product-options-for named-search-slug)
             {:data-url url})]])]))))

(defn reviews-component [{:keys [named-search-slug] :as args} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-" named-search-slug)}
     (om/build reviews-component-inner args opts)])))

(defn reviews-summary-component-inner [{:keys [loaded? named-search-slug url]} owner opts]
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
             (product-options-for named-search-slug)
             {:data-url url})]
           [:.yotpo.QABottomLine
            (product-options-for named-search-slug)]])]))))

(defn reviews-summary-component [{:keys [named-search-slug] :as args} owner opts]
  (om/component
   (html
    [:div {:key (str "reviews-summary-" named-search-slug)}
     (om/build reviews-summary-component-inner args opts)])))

(defn query [data]
  (when-let [{:keys [legacy/named-search-slug]} (products/current-product data)]
    {:named-search-slug named-search-slug
     :url               (routes/current-path data)
     :loaded?           (get-in data keypaths/loaded-reviews)}))
