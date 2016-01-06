(ns storefront.components.reviews
  (:require [sablono.core :refer-macros [html]]
            [om.core :as om]
            [storefront.accessors.taxons :as taxons]
            [storefront.events :as events]
            [storefront.hooks.experiments :as experiments]
            [storefront.messages :refer [send]]
            [storefront.routes :as routes]
            [storefront.keypaths :as keypaths]
            [storefront.utils.query :as query]))

(def product-options-by-taxon
  {:straight   {:data-product-id  13
                :data-name        "Brazilian Natural Straight Hair"
                :data-description "Perfect for those who mostly wear straight hair extensions. After washing, our Brazilian Straight hair extensions have just a hint of wave that can be easily flat ironed. Lightweight and silky. Holds a curl beautifully. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Can be easily colored or bleached (consult a licensed cosmetologist)."
                :data-image-url   "http://s3.amazonaws.com/yotpo-images-production/Product/6414704/6154340/thumb.jpg"}
   :loose-wave {:data-product-id 1
                :data-name "Brazilian Loose Wave Hair"
                :data-description "Our Brazilian Loose Wave human hair has been steam processed so you can enjoy long-lasting beautiful waves. The waves are tighter than our body wave texture but looser than our deep wave texture. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Excessive flat-ironing will loosen the curls over time. These hair extensions can be colored (consult a licensed cosmetologist)."
                :data-image-url "http://s3.amazonaws.com/yotpo-images-production/Product/6414703/6154339/thumb.jpg"}
   :deep-wave  {:data-product-id 11
                :data-name "Brazilian Deep Wave Hair"
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
   :body-wave  {:data-product-id 3
                :data-name "Malaysian Body Wave Hair"
                :data-description "Our Malaysian Body Wave human hair has been steam processed so you can enjoy long-lasting beautiful waves. This texture has an "S" shape wave that is soft and bouncy. Each bundle weighs 3.5 ounces. Available in a natural dark brown color equivalent to color 1B. Excessive flat-ironing will loosen the curls over time. These hair extensions can be colored or bleached (consult a licensed cosmetologist)."
                :data-image-url "http://s3.amazonaws.com/yotpo-images-production/Product/6414706/6318976/thumb.jpg"}
   :curly      {:data-product-id 9
                :data-name "Brazilian Curly Hair"
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
                :data-image-url "http://s3.amazonaws.com/yotpo-images-production/Product/6414708/6154343/thumb.jpg"}
   :blonde     {:data-product-id 15
                :data-name "Indian Natural Straight Blonde Hair"
                :data-description "Premier Indian Straight Blonde Hair (613)
Material:High Quality 100% Virgin Human Hair
Color: 613
Expected Longevity: 3 months +
(Depending on care)
 Texture & Description:
It's 100% Human hair, and has been pre-lightened to a natural 613 (Lightest Blonde)
Lightweight and silky.
Holds a curl beautifully.
Each bundle weighs 3.5 ounces.
These hair extensions can be colored (consult a licensed cosmetologist).
Lengths: 14\" to 26\""
                :data-image-url "http://s3.amazonaws.com/yotpo-images-production/Product/6575194/6318987/thumb.jpg"}
   :closures   {:data-product-id 28
                :data-name "Peruvian Natural Straight Silk Closure"
                :data-description "Our 4x4 silk closures create an illusion of a scalp with a natural density, and is used to leave more of your own natural hair protected. The silk closure has a natural brown scalp color and can be restyled, re-parted, cut, and colored."
                :data-image-url "http://s3.amazonaws.com/yotpo-images-production/Product/6575214/6319000/thumb.jpg"}})

(defn product-options-for [taxon]
  (get product-options-by-taxon (keyword (taxons/taxon-path-for taxon))))

(defn reviews-component [data owner {taxon :taxon}]
  (reify
    om/IDidMount
    (did-mount [_] (send data events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (send data events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div.product-reviews
        [:div.yotpo.yotpo-main-widget
         (merge
          (product-options-for taxon)
          {:data-url (routes/current-path @data)})]]))))

(defn reviews-summary-component [data owner {taxon :taxon}]
  (reify
    om/IDidMount
    (did-mount [_] (send data events/reviews-component-mounted))
    om/IWillUnmount
    (will-unmount [_] (send data events/reviews-component-will-unmount))
    om/IRender
    (render [_]
      (html
       [:div.product-reviews-summary
        [:div.yotpo.bottomLine.star-summary
         (merge
          (product-options-for taxon)
          {:data-url (routes/current-path @data)})]
        [:div.yotpo.QABottomLine.question-summary
          (product-options-for taxon)]]))))
