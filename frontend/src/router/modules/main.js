const MainPage = () => import(/* webpackChunkName: "js/pages/main", webpackPrefetch: true */'@/views/main/MainPage')

const mainRoutes = [
  {
    path: '/',
    component: MainPage
  }
]
export default mainRoutes
