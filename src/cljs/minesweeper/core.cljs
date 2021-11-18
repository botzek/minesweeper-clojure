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
   (let [settings (:minesweeper/settings db)
         mine-generator (:minesweeper/mine-generator db)]
     (assoc db
            :minesweeper/game (game/make-game settings mine-generator)))))

(rf/reg-event-db
 :minesweeper/set-difficulty
 (fn [db [_ difficulty]]
   (assoc db :minesweeper/settings (get game/difficulties (keyword difficulty)))))

(rf/reg-event-db
 :minesweeper/set-mine-generator
 (fn [db [_ generator]]
   (assoc db :minesweeper/mine-generator (get-in game/mine-generators [(keyword generator) :generator]))))


(rf/reg-event-db
 :minesweeper/reveal
 (fn [db [_ cell]]
   (update db :minesweeper/game game/reveal cell)))

(rf/reg-event-db
 :minesweeper/take-step
 (fn [db _]
   (update db :minesweeper/game game/take-step)))

(rf/reg-event-db
 :minesweeper/toggle-flag
 (fn [db [_ cell]]
   (update db :minesweeper/game game/toggle-flag cell)))

(rf/reg-sub
 :minesweeper/game
 (fn [db _]
   (-> db :minesweeper/game)))

(rf/reg-sub
 :minesweeper/solvable?
 :<- [:minesweeper/game]
 (fn [game _]
   (and (= :playing (:status game))
        (-> game game/next-step some?))))

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
     {:style {:width "25px" :height "25px" :float :left :border ["solid 1px"] :text-align :center}}
      (case state
        :flagged
        [:div {:style {:background-color "#CCC" :cursor :pointer}
                  :on-context-menu #(do
                                      (when (= :playing game-status)
                                        (rf/dispatch [:minesweeper/toggle-flag cell]))
                                      (.preventDefault %))
                  :dangerouslySetInnerHTML {:__html "&#x1f6a9;"}}]

        :hidden
        [:div {:style {:width "100%" :height "100%" :background-color "#CCC" :cursor :pointer}
                  :on-click #(when (= :playing game-status)
                               (rf/dispatch [:minesweeper/reveal cell]))
                  :on-context-menu #(do
                                      (when (= :playing game-status)
                                        (rf/dispatch [:minesweeper/toggle-flag cell]))
                                      (.preventDefault %))
                  :dangerouslySetInnerHTML{:__html "&nbsp;"}}]

        :exploded
        [:span {:dangerouslySetInnerHTML {:__html "&#x1f4a5;"}}]

        0
        ""
        (str state))]))

(defn- settings []
  [:div
   [:div.title.is-4 "Game Settings"]
   [:div.pl-2
    [:label.label.is-4 "Difficulty"]
    [:div.pl-4
     [:table.table
      [:thead>tr
       [:th]
       [:th "Rows"]
       [:th "Columns"]
       [:th "Mines"]]
      [:tbody
       (for [[k {:keys [name rows cols mines]}] game/difficulties]
         ^{:key {:difficulty k}}
         [:tr
          [:td
           [:label.label
            [:input
             {:type :radio
              :name :settings
              :value k
              :defaultChecked (= k :trivial)
              :on-change #(rf/dispatch [:minesweeper/set-difficulty (.. % -target -value)])}]
            name]]
          [:td rows]
          [:td cols]
          [:td mines]])]]]]
   [:div.pl-2
    [:label.label.is-4.pt-4 "Mine Generator"]
    [:div.select.pl-4
     [:select
      {:on-change #(rf/dispatch [:minesweeper/set-mine-generator (.. % -target -value)])}
      (for [[k {:keys [name]}] game/mine-generators]
        ^{:key {:mine-generator k}}
        [:option {:value k} name])]]]])

(defn- helpers []
  (r/with-let [interval (r/atom nil)]
    [:<>
     [:button.button.m-4
      {:disabled (not @(rf/subscribe [:minesweeper/solvable?]))
       :on-click #(if (some? @interval)
                    (do
                      (.clearInterval js/window @interval)
                      (reset! interval nil))
                    (reset! interval
                            (.setInterval
                             js/window
                             (fn []
                               (if (and (= :playing @(rf/subscribe [:minesweeper/game-status]))
                                        @(rf/subscribe [:minesweeper/solvable?]))
                                 (rf/dispatch [:minesweeper/take-step])
                                 (do
                                   (.clearInterval js/window @interval)
                                   (reset! interval nil))))
                             100)))}

      (if (nil? @interval)
        "Solve"
        "Stop")]
     [:button.button.m-4
      {:disabled (or (not @(rf/subscribe [:minesweeper/solvable?]))
                     (some? @interval))
       :on-click #(rf/dispatch [:minesweeper/take-step])} "Take Step"]]))

(defn minesweeper []
  (let [game-status @(rf/subscribe [:minesweeper/game-status])]
    [:section.section>div.container
     (let [rows @(rf/subscribe [:minesweeper/rows])
           cols @(rf/subscribe [:minesweeper/cols])]
       [:div
        [:div {:style {:height "30px"}}
         (case game-status
           :won
           "You Won!"

           :lost
           "You Lost!"

           "")]
        [:div {:style {:border ["solid 1px"] :display :inline-block}}
         (for [r (range rows)]
           ^{:key {:row r}}
           [:div
            (for [c (range cols)]
              (let [cell {:row r :col c}]
                ^{:key cell} [minesweeper-cell cell game-status]))
            [:div {:style {:clear :both}}]])]])
     [helpers]
     [:div
      [:button.button.is-primary.m-4
       {:on-click #(rf/dispatch [:minesweeper/new-game])} "New Game"]]
     [settings]]))

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
  (rf/dispatch [:minesweeper/set-difficulty :trivial])
  (rf/dispatch [:minesweeper/set-mine-generator :standard])
  (rf/dispatch [:minesweeper/new-game])
  (mount-components))
