const StationPage = () => import(/* webpackChunkName: "js/pages/station" */'@/views/station/StationPage')

const stationRoutes = [
  {
    path: '/stations',
    component: StationPage
  }
]
export default stationRoutes
