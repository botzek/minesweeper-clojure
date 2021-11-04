(ns minesweeper.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [minesweeper.game :as game]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe]
    [clojure.string :as string])
  (:import goog.History))


(rf/reg-event-db
 :minesweeper/new-game
 (fn [db [_]]
   (let [settings (:minesweeper/settings db)]
   (assoc db :minesweeper/game (game/make-game settings)))))

(rf/reg-event-db
 :minesweeper/set-settings-pre-set
 (fn [db [_ pre-set]]
   (assoc db :minesweeper/settings (get game/pre-sets (keyword pre-set)))))

(rf/reg-event-db
 :minesweeper/reveal
 (fn [db [_ cell]]
   (update db :minesweeper/game game/reveal cell)))

(rf/reg-event-db
 :minesweeper/toggle-flag
 (fn [db [_ cell]]
   (update db :minesweeper/game game/toggle-flag cell)))

(rf/reg-sub
 :minesweeper/game
 (fn [db _]
   (-> db :minesweeper/game)))

(rf/reg-sub
 :minesweeper/rows
 (fn [db _]
   (-> db :minesweeper/game :rows)))

(rf/reg-sub
 :minesweeper/game-status
 (fn [db _]
   (-> db :minesweeper/game :status)))

(rf/reg-sub
 :minesweeper/cols
 (fn [db _]
   (-> db :minesweeper/game :cols)))

(rf/reg-sub
 :minesweeper/cell
 (fn [db [_ cell]]
   (-> db
       :minesweeper/game
       :board
       (get cell))))

(rf/reg-sub
 :minesweeper/settings-rows
 (fn [db [_]]
   (-> db :minesweeper/settings :rows)))

(rf/reg-sub
 :minesweeper/settings-cols
 (fn [db [_]]
   (-> db :minesweeper/settings :cols)))

(rf/reg-sub
 :minesweeper/settings-mines
 (fn [db [_]]
   (-> db :minesweeper/settings :mines)))


(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar [] 
  (r/with-let [expanded? (r/atom false)]
              [:nav.navbar.is-info>div.container
               [:div.navbar-brand
                [:a.navbar-item {:href "/" :style {:font-weight :bold}} "minesweeper"]
                [:span.navbar-burger.burger
                 {:data-target :nav-menu
                  :on-click #(swap! expanded? not)
                  :class (when @expanded? :is-active)}
                 [:span][:span][:span]]]]))

(defn minesweeper-cell [cell game-status]
  (let [state @(rf/subscribe [:minesweeper/cell cell])]
    [:div
     {:style {:width "20px" :height "20px" :float :left :border ["solid 1px"] :text-align :center}}
      (case state
        :flagged
        [:button {:style {:width "100%" :height "100%" :padding "0px" :margin "0px"}
                  :on-context-menu #(do
                                      (when (= :playing game-status)
                                        (rf/dispatch [:minesweeper/toggle-flag cell]))
                                      (.preventDefault %))
                  :dangerouslySetInnerHTML {:__html "&#x1f6a9;"}}]

        :hidden
        [:button {:style {:width "100%" :height "100%" :padding "0px" :margin "0px"}
                  :on-click #(when (= :playing game-status)
                               (rf/dispatch [:minesweeper/reveal cell]))
                  :on-context-menu #(do
                                      (when (= :playing game-status)
                                        (rf/dispatch [:minesweeper/toggle-flag cell]))
                                      (.preventDefault %))}]

        :exploded
        [:span {:dangerouslySetInnerHTML {:__html "&#x1f4a5;"}}]

        0
        ""
        (str state))]))

(defn minesweeper []
  [:section.section>div.container
   (let [game-status @(rf/subscribe [:minesweeper/game-status])
         rows @(rf/subscribe [:minesweeper/rows])
         cols @(rf/subscribe [:minesweeper/cols])]
     [:div
      [:div {:style {:height "30px"}}
       (case game-status
         :won
         "You Won!"

         :lost
         "You Lost!"

         "")]
      [:div
       (for [r (range rows)]
         ^{:key {:row r}}
         [:div
          (for [c (range cols)]
            (let [cell {:row r :col c}]
              ^{:key cell} [minesweeper-cell cell game-status]))
          [:div {:style {:clear :both}}]])]])
   [:button.button.is-primary.mt-4
    {:on-click #(rf/dispatch [:minesweeper/new-game])} "New Game"]
   [:table.table
    [:thead>tr
     [:th]
     [:th "Rows"]
     [:th "Columns"]
     [:th "Mines"]]

    [:tbody
     (for [[k {:keys [name rows cols mines]}] game/pre-sets]
       ^{:key {:pre-set k}}
       [:tr
        [:td
         [:label.label
          [:input
           {:type :radio
            :name :settings
            :value k
            :defaultChecked (= k :trivial)
            :on-change #(rf/dispatch [:minesweeper/set-settings-pre-set (.. % -target -value)])}]
          name]]
        [:td rows]
        [:td cols]
        [:td mines]])]]])

(defn home-page []
  [:div
   [navbar]
   [minesweeper]])

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'home-page] (.getElementById js/document "app")))

(defn init! []
  (rf/dispatch [:minesweeper/set-settings-pre-set :trivial])
  (rf/dispatch [:minesweeper/new-game])
  (mount-components))
