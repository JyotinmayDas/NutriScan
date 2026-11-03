import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Route path & name constants for NutriScan navigation.
class AppRoutes {
  const AppRoutes._();

  static const String splash = '/splash';
  static const String auth = '/auth';
  static const String onboarding = '/onboarding';
  static const String dashboard = '/dashboard';
  static const String foodScanner = '/scanner';
  static const String foodDiary = '/diary';
  static const String progress = '/progress';
  static const String profile = '/profile';
  static const String waterTracker = '/water-tracker';
}

/// Authentication & onboarding state notifier used by [AppRouter] redirects.
class AuthNavigationNotifier extends ChangeNotifier {
  bool _isAuthenticated = false;
  bool _hasCompletedOnboarding = true;

  bool get isAuthenticated => _isAuthenticated;
  bool get hasCompletedOnboarding => _hasCompletedOnboarding;

  void updateAuthState({
    required bool isAuthenticated,
    bool? hasCompletedOnboarding,
  }) {
    _isAuthenticated = isAuthenticated;
    if (hasCompletedOnboarding != null) {
      _hasCompletedOnboarding = hasCompletedOnboarding;
    }
    notifyListeners();
  }
}

/// Central GoRouter configuration connecting the application entry point
/// to authentication, onboarding, and main dashboard shell views.
class AppRouter {
  AppRouter({
    required this.authNotifier,
    this.authScreenBuilder,
    this.onboardingScreenBuilder,
    this.dashboardScreenBuilder,
    this.scannerScreenBuilder,
    this.diaryScreenBuilder,
    this.progressScreenBuilder,
    this.profileScreenBuilder,
    this.waterTrackerScreenBuilder,
  });

  final AuthNavigationNotifier authNotifier;
  final WidgetBuilder? authScreenBuilder;
  final WidgetBuilder? onboardingScreenBuilder;
  final WidgetBuilder? dashboardScreenBuilder;
  final WidgetBuilder? scannerScreenBuilder;
  final WidgetBuilder? diaryScreenBuilder;
  final WidgetBuilder? progressScreenBuilder;
  final WidgetBuilder? profileScreenBuilder;
  final WidgetBuilder? waterTrackerScreenBuilder;

  final GlobalKey<NavigatorState> rootNavigatorKey =
      GlobalKey<NavigatorState>(debugLabel: 'rootNavigator');
  final GlobalKey<NavigatorState> shellNavigatorKey =
      GlobalKey<NavigatorState>(debugLabel: 'shellNavigator');

  late final GoRouter router = GoRouter(
    navigatorKey: rootNavigatorKey,
    initialLocation: AppRoutes.dashboard,
    refreshListenable: authNotifier,
    redirect: (BuildContext context, GoRouterState state) {
      final bool loggedIn = authNotifier.isAuthenticated;
      final bool isGoingToAuth = state.matchedLocation == AppRoutes.auth;
      final bool isGoingToOnboarding =
          state.matchedLocation == AppRoutes.onboarding;

      if (!loggedIn) {
        return isGoingToAuth ? null : AppRoutes.auth;
      }

      if (!authNotifier.hasCompletedOnboarding && !isGoingToOnboarding) {
        return AppRoutes.onboarding;
      }

      if (loggedIn && isGoingToAuth) {
        return AppRoutes.dashboard;
      }

      return null;
    },
    routes: <RouteBase>[
      GoRoute(
        path: AppRoutes.auth,
        name: 'auth',
        builder: (BuildContext context, GoRouterState state) {
          return authScreenBuilder?.call(context) ??
              const _RoutePlaceholderView(title: 'Sign In to NutriScan');
        },
      ),
      GoRoute(
        path: AppRoutes.onboarding,
        name: 'onboarding',
        builder: (BuildContext context, GoRouterState state) {
          return onboardingScreenBuilder?.call(context) ??
              const _RoutePlaceholderView(title: 'Profile & Calorie Setup');
        },
      ),
      StatefulShellRoute.indexedStack(
        builder: (
          BuildContext context,
          GoRouterState state,
          StatefulNavigationShell navigationShell,
        ) {
          return NutriScanAppShell(navigationShell: navigationShell);
        },
        branches: <StatefulShellBranch>[
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(
                path: AppRoutes.dashboard,
                name: 'dashboard',
                builder: (BuildContext context, GoRouterState state) {
                  return dashboardScreenBuilder?.call(context) ??
                      const _RoutePlaceholderView(title: 'Home Dashboard');
                },
              ),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(
                path: AppRoutes.foodDiary,
                name: 'diary',
                builder: (BuildContext context, GoRouterState state) {
                  return diaryScreenBuilder?.call(context) ??
                      const _RoutePlaceholderView(title: 'Food Diary & Database');
                },
              ),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(
                path: AppRoutes.foodScanner,
                name: 'scanner',
                builder: (BuildContext context, GoRouterState state) {
                  return scannerScreenBuilder?.call(context) ??
                      const _RoutePlaceholderView(title: 'Smart Food Scanner');
                },
              ),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(
                path: AppRoutes.progress,
                name: 'progress',
                builder: (BuildContext context, GoRouterState state) {
                  return progressScreenBuilder?.call(context) ??
                      const _RoutePlaceholderView(title: 'Progress & Meal Plan');
                },
              ),
            ],
          ),
          StatefulShellBranch(
            routes: <RouteBase>[
              GoRoute(
                path: AppRoutes.profile,
                name: 'profile',
                builder: (BuildContext context, GoRouterState state) {
                  return profileScreenBuilder?.call(context) ??
                      const _RoutePlaceholderView(title: 'Profile & Settings');
                },
              ),
            ],
          ),
        ],
      ),
      GoRoute(
        parentNavigatorKey: rootNavigatorKey,
        path: AppRoutes.waterTracker,
        name: 'water_tracker',
        builder: (BuildContext context, GoRouterState state) {
          return waterTrackerScreenBuilder?.call(context) ??
              const _RoutePlaceholderView(title: 'Water Intake Tracker');
        },
      ),
    ],
  );
}

/// Primary bottom navigation shell hosting the 5 top-level destinations.
class NutriScanAppShell extends StatelessWidget {
  const NutriScanAppShell({
    super.key,
    required this.navigationShell,
  });

  final StatefulNavigationShell navigationShell;

  void _onTapBranch(int index) {
    navigationShell.goBranch(
      index,
      initialLocation: index == navigationShell.currentIndex,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: navigationShell,
      bottomNavigationBar: NavigationBar(
        selectedIndex: navigationShell.currentIndex,
        onDestinationSelected: _onTapBranch,
        destinations: const <NavigationDestination>[
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Home',
          ),
          NavigationDestination(
            icon: Icon(Icons.menu_book_outlined),
            selectedIcon: Icon(Icons.menu_book),
            label: 'Diary',
          ),
          NavigationDestination(
            icon: Icon(Icons.center_focus_strong_outlined),
            selectedIcon: Icon(Icons.center_focus_strong),
            label: 'Scan',
          ),
          NavigationDestination(
            icon: Icon(Icons.insights_outlined),
            selectedIcon: Icon(Icons.insights),
            label: 'Progress',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Profile',
          ),
        ],
      ),
    );
  }
}

class _RoutePlaceholderView extends StatelessWidget {
  const _RoutePlaceholderView({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: Center(
        child: Text(
          title,
          style: Theme.of(context).textTheme.headlineSmall,
        ),
      ),
    );
  }
}
