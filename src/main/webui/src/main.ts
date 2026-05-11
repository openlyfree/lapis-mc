import './style.css'
import serverBar from './components/serverbar.html?raw'
import { getServers } from './api'

document.querySelector('#app')!.innerHTML = `
<div id="sidebar">
</div>

<div id="serverControls>
</div>
`
document.querySelector('#sidebar')!.innerHTML = serverBar

getServers();