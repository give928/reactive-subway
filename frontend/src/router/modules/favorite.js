const Favorites = () => import(/* webpackChunkName: "js/pages/favorite" */'@/views/favorite/Favorites')

const favoriteRoutes = [
  {
    path: '/favorites',
    component: Favorites
  }
]
export default favoriteRoutes
