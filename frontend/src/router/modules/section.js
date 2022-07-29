const SectionPage = () => import(/* webpackChunkName: "js/pages/section" */'@/views/section/SectionPage')

const sectionRoutes = [
  {
    path: '/sections',
    component: SectionPage
  }
]
export default sectionRoutes
