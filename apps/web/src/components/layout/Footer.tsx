import { Link } from 'react-router-dom';

export default function Footer() {
  return (
    <footer className="bg-gray-900 text-gray-400">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <div className="w-8 h-8 bg-primary-600 rounded-full flex items-center justify-center">
                <span className="text-white font-bold text-sm">SB</span>
              </div>
              <span className="font-bold text-lg text-white">SkillBridge</span>
            </div>
            <p className="text-sm">
              Platforma koja povezuje klijente sa vrhunskim freelancerima.
            </p>
          </div>
          <div>
            <h4 className="text-white font-medium mb-4">Kategorije</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/gigs?category=dizajn" className="hover:text-white">Dizajn</Link></li>
              <li><Link to="/gigs?category=programiranje" className="hover:text-white">Programiranje</Link></li>
              <li><Link to="/gigs?category=marketing" className="hover:text-white">Marketing</Link></li>
              <li><Link to="/gigs?category=video" className="hover:text-white">Video editing</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-medium mb-4">Podrška</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/how-it-works" className="hover:text-white">Kako funkcioniše</Link></li>
              <li><Link to="/help" className="hover:text-white">Pomoć</Link></li>
              <li><Link to="/terms" className="hover:text-white">Uslovi korištenja</Link></li>
              <li><Link to="/privacy" className="hover:text-white">Privatnost</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="text-white font-medium mb-4">Za freelancere</h4>
            <ul className="space-y-2 text-sm">
              <li><Link to="/register" className="hover:text-white">Postani freelancer</Link></li>
              <li><Link to="/gigs" className="hover:text-white">Pretraži usluge</Link></li>
            </ul>
          </div>
        </div>
        <div className="border-t border-gray-800 mt-8 pt-8 text-sm text-center">
          &copy; {new Date().getFullYear()} SkillBridge. Sva prava zadržana.
        </div>
      </div>
    </footer>
  );
}
