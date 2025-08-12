import { SuperMetroidProvider } from './context/SuperMetroidContext';
import { Tracker } from './components/Layout/Tracker';

function App() {
  return (
    <SuperMetroidProvider>
      <Tracker />
    </SuperMetroidProvider>
  );
}

export default App;
