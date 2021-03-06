/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015-2016 ForgeRock AS.
 * Portions Copyright 2018 Wren Security.
 */

package org.forgerock.services.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.forgerock.http.routing.RouteMatchers.selfApiMatcher;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.forgerock.http.ApiProducer;
import org.forgerock.http.Handler;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.routing.RouteMatchers;
import org.forgerock.http.routing.RoutingMode;
import org.forgerock.http.routing.Version;
import org.forgerock.services.context.Context;
import org.forgerock.services.descriptor.Describable;
import org.forgerock.util.Pair;
import org.forgerock.util.promise.NeverThrowsException;
import org.forgerock.util.promise.Promise;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class AbstractRouterTest {

    private TestAbstractRouter router;

    private Request request;
    @Mock
    private Context context;
    @Mock
    private DescribedHandler routeOneHandler;
    @Mock
    private DescribedHandler routeTwoHandler;
    @Mock
    private RouteMatcher<Request> routeOneMatcher;
    @Mock
    private RouteMatcher<Request> routeTwoMatcher;
    @Mock
    private RouteMatch routeOneRouteMatch;
    @Mock
    private RouteMatch routeTwoRouteMatch;
    private Answer<Object> transformApiAnswer = new Answer<Object>() {
        @Override
        public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
            return invocationOnMock.getArguments()[0];
        }
    };

    @BeforeMethod
    public void setup() throws Exception {
        router = new TestAbstractRouter();

        request = new Request().setUri("http://localhost/path?query");
        MockitoAnnotations.initMocks(this);
        when(routeOneMatcher.transformApi(any(), any())).thenAnswer(transformApiAnswer);
        when(routeTwoMatcher.transformApi(any(), any())).thenAnswer(transformApiAnswer);
    }

    @Test
    public void shouldSelectRouteTwo() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, false);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, true);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeTwoHandler);
    }

    @Test
    public void shouldSelectRouteOne() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeOneHandler);
    }

    @Test
    public void shouldSelectRouteTwoWhenRouteOneDoesNotMatch() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, false);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, true);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeTwoHandler);
    }

    @Test
    public void shouldSelectRouteOneWhenRouteTwoDoesNotMatch() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(routeOneHandler);
    }

    @Test
    public void shouldSelectDefaultRouteWhenNoRouteMatches() throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeOneRouteMatch = null;
        RouteMatch routeTwoRouteMatch = null;

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute.getSecond()).isEqualTo(defaultRouteHandler);
    }

    @Test
    public void shouldReturnNullWhenNoRouteMatchesWithNoDefaultRouteSet() throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeOneRouteMatch = null;
        RouteMatch routeTwoRouteMatch = null;

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        Pair<Context, Handler> bestRoute = router.getBestRoute(context, request);

        //Then
        assertThat(bestRoute).isNull();
    }

    @Test(expectedExceptions = IncomparableRouteMatchException.class)
    public void shouldThrowIncomparableRouteMatchExceptionWhenRouteMatchesCannotBeCompared()
            throws IncomparableRouteMatchException {

        //Given
        RouteMatch routeOneRouteMatch = mock(RouteMatchB.class);
        RouteMatch routeTwoRouteMatch = mock(RouteMatchA.class);

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupIncomparableRouteMatch(routeOneRouteMatch, routeTwoRouteMatch);
        setupIncomparableRouteMatch(routeTwoRouteMatch, routeOneRouteMatch);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        router.getBestRoute(context, request);

        //Then
        failBecauseExceptionWasNotThrown(IncomparableRouteMatchException.class);
    }

    @Test
    public void creatingRouterFromExistingRouterShouldCopyAllRoutes() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        TestAbstractRouter newRouter = new TestAbstractRouter(router);

        //Then
        assertThat(newRouter.getRoutes()).contains(
                entry(routeOneMatcher, routeOneHandler),
                entry(routeTwoMatcher, routeTwoHandler));
        assertThat(newRouter.getDefaultRoute()).isEqualTo(defaultRouteHandler);
    }

    @Test
    public void shouldAddAllRoutes() throws IncomparableRouteMatchException {

        //Given
        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        TestAbstractRouter newRouter = new TestAbstractRouter();

        //When
        newRouter.addAllRoutes(router);

        //Then
        assertThat(newRouter.getRoutes()).contains(
                entry(routeOneMatcher, routeOneHandler),
                entry(routeTwoMatcher, routeTwoHandler));
    }

    @Test
    public void shouldRemoveAllRoutes() {

        //Given
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        Handler defaultRouteHandler = mock(Handler.class);
        router.setDefaultRoute(defaultRouteHandler);

        //When
        router.removeAllRoutes();

        //Then
        assertThat(router.getRoutes()).isEmpty();
        assertThat(router.getDefaultRoute()).isEqualTo(defaultRouteHandler);
    }

    @Test
    public void shouldRemoveRoute() {

        //Given
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        boolean isModified = router.removeRoute(routeOneMatcher);

        //Then
        assertThat(isModified).isTrue();
        assertThat(router.getRoutes()).contains(
                entry(routeTwoMatcher, routeTwoHandler));
    }

    @Test
    public void shouldNotRemoveRouteIfNotRegistered() {

        //Given
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        //When
        boolean isModified = router.removeRoute(routeOneMatcher);

        //Then
        assertThat(isModified).isFalse();
        assertThat(router.getRoutes()).contains(
                entry(routeTwoMatcher, routeTwoHandler));
    }

    @Test
    public void shouldSubscribeToApiChanges() {
        // When
        router.addRoute(routeOneMatcher, routeOneHandler);

        // Then
        verify(routeOneHandler).addDescriptorListener(any(Describable.Listener.class));
    }

    @Test
    public void shouldNotifyOnRouterDescribableAddition() {
        // Given
        router.api(new StringApiProducer());
        Describable.Listener listener = mock(Describable.Listener.class);
        router.addDescriptorListener(listener);

        // When
        router.addRoute(routeOneMatcher, routeOneHandler);

        // Then
        verify(listener).notifyDescriptorChange();
    }

    @Test
    public void shouldNotNotifyBeforeInitialisedWithProducer() {
        // Given
        Describable.Listener listener = mock(Describable.Listener.class);
        router.addDescriptorListener(listener);

        // When
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.removeRoute(routeOneMatcher);

        // Then
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void shouldNotifyOnRouterDescribableRemoval() {
        // Given
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.api(new StringApiProducer());
        Describable.Listener listener = mock(Describable.Listener.class);
        router.addDescriptorListener(listener);

        // When
        router.removeRoute(routeOneMatcher);

        // Then
        verify(listener).notifyDescriptorChange();
    }

    @Test
    public void shouldFetchDescriptorsOnceContextProvided() {
        // Given
        router.addRoute(routeOneMatcher, routeOneHandler);
        router.addRoute(routeTwoMatcher, routeTwoHandler);
        given(routeOneHandler.api(any())).willReturn("one");
        given(routeTwoHandler.api(any())).willReturn("two");

        // When
        String api = router.api(new StringApiProducer());

        // Then
        verify(routeOneHandler).api(any());
        verify(routeTwoHandler).api(any());
        assertThat(api).isEqualTo("[one, two]");
    }

    @Test
    public void shouldRouteApiRequest() throws Exception {
        // Given
        router.addRoute(routeOneMatcher, new TestAbstractRouter().setDefaultRoute(routeOneHandler));
        router.addRoute(routeTwoMatcher, routeTwoHandler);

        given(routeOneHandler.api(any())).willReturn("one");
        given(routeOneHandler.handleApiRequest(any(), eq(request))).willReturn("one");
        given(routeTwoHandler.api(any())).willReturn("two");

        router.api(new StringApiProducer());

        given(routeOneMatcher.evaluate(any(), eq(request))).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(any(), eq(request))).willReturn(routeTwoRouteMatch);

        setupRouteMatch(routeOneRouteMatch, routeTwoRouteMatch, true);
        setupRouteMatch(routeTwoRouteMatch, routeOneRouteMatch, false);

        // When
        String api = router.handleApiRequest(context, request);

        // Then
        assertThat(api).isEqualTo("one");
    }

    @Test
    public void shouldHandleApiRequest() throws Exception {
        // Given
        router.addRoute(routeOneMatcher, new TestAbstractRouter().setDefaultRoute(routeOneHandler));
        router.addRoute(routeTwoMatcher, routeTwoHandler);
        given(routeOneHandler.api(any())).willReturn("one");
        given(routeTwoHandler.api(any())).willReturn("two");
        router.api(new StringApiProducer());

        given(routeOneMatcher.evaluate(context, request)).willReturn(null);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(null);

        // When
        String api = router.handleApiRequest(context, request.setUri(""));

        // Then
        assertThat(api).isEqualTo("[[one], two]");
    }

    @Test
    public void shouldRouteApiRequestToDefaultIfNoneMatch() throws Exception {
        // Given
        router.addRoute(routeOneMatcher, new TestAbstractRouter().setDefaultRoute(routeOneHandler));
        router.setDefaultRoute(routeTwoHandler);
        given(routeOneHandler.api(any())).willReturn("one");
        given(routeOneHandler.handleApiRequest(context, request)).willReturn("one");
        given(routeTwoHandler.api(any())).willReturn("two");
        given(routeTwoHandler.handleApiRequest(context, request)).willReturn("two");
        router.api(new StringApiProducer());

        given(routeOneMatcher.evaluate(context, request)).willReturn(null);

        // When
        String api = router.handleApiRequest(context, request);

        // Then
        assertThat(api).isEqualTo("two");
    }

    @Test
    public void shouldHandleApiRequestIfRouterTargeted() throws Exception {
        // Given
        router.addRoute(routeOneMatcher, new TestAbstractRouter().setDefaultRoute(routeOneHandler));
        router.addRoute(routeTwoMatcher, new TestAbstractRouter().setDefaultRoute(routeTwoHandler));
        given(routeOneHandler.api(any())).willReturn("one");
        given(routeOneHandler.handleApiRequest(context, request)).willReturn("one");
        given(routeTwoHandler.api(any())).willReturn("two");
        given(routeTwoHandler.handleApiRequest(context, request)).willReturn("two");
        router.api(new StringApiProducer());

        given(routeOneMatcher.evaluate(context, request)).willReturn(null);

        request.setUri("http://localhost");

        // When
        String api = router.handleApiRequest(context, request);

        // Then
        assertThat(api).isEqualTo("[[one], [two]]");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void shouldThrowExceptionWhenNoRouteMatchTheApiRequest() throws Exception {
        // Given
        router.api(new StringApiProducer());
        request.setUri("/test");

        // When
        router.handleApiRequest(context, request);
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void shouldThrowExceptionForNoApiSupport() throws Exception {
        // Given
        router.addRoute(routeOneMatcher, mock(Handler.class));
        router.addRoute(routeTwoMatcher, routeTwoHandler);
        given(routeTwoHandler.api(any(ApiProducer.class))).willReturn("two");
        router.api(new StringApiProducer());

        given(routeOneMatcher.evaluate(context, request)).willReturn(routeOneRouteMatch);
        given(routeTwoMatcher.evaluate(context, request)).willReturn(routeTwoRouteMatch);

        setupIncomparableRouteMatch(routeOneRouteMatch, routeTwoRouteMatch);
        setupIncomparableRouteMatch(routeTwoRouteMatch, routeOneRouteMatch);

        request.setUri("/test");

        // When
        router.handleApiRequest(context, request);
    }

    private void setupRouteMatch(RouteMatch thisRouteMatch, RouteMatch thatRouteMatch,
            boolean isThisRouteBetter) throws IncomparableRouteMatchException {
        if (thisRouteMatch != null) {
            given(thisRouteMatch.isBetterMatchThan(null)).willReturn(true);
            if (thatRouteMatch != null) {
                given(thisRouteMatch.isBetterMatchThan(thatRouteMatch)).willReturn(isThisRouteBetter);
            }
        }
    }

    private void setupIncomparableRouteMatch(RouteMatch thisRouteMatch, RouteMatch thatRouteMatch)
            throws IncomparableRouteMatchException {
        if (thisRouteMatch != null) {
            given(thisRouteMatch.isBetterMatchThan(null)).willReturn(true);
            if (thatRouteMatch != null) {
                doThrow(IncomparableRouteMatchException.class).when(thisRouteMatch).isBetterMatchThan(thatRouteMatch);
            }
        }
    }

    private static final class TestAbstractRouter
            extends AbstractRouter<TestAbstractRouter, Request, Handler, String>
            implements Handler {

        private Handler selfApiHandler = new SelfApiHandler();

        protected TestAbstractRouter() {
            super();
        }

        protected TestAbstractRouter(AbstractRouter<TestAbstractRouter, Request, Handler, String> router) {
            super(router);
        }

        @Override
        protected TestAbstractRouter getThis() {
            return this;
        }

        @Override
        protected Pair<RouteMatcher<Request>, Handler> getSelfApiHandler() {
            return Pair.of(selfApiMatcher(), selfApiHandler);
        }

        @Override
        protected RouteMatcher<Request> uriMatcher(RoutingMode mode, String pattern) {
            return RouteMatchers.requestUriMatcher(mode, pattern);
        }

        @Override
        public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
            throw new UnsupportedOperationException();
        }

        private class SelfApiHandler implements Handler, Describable<String, Request> {

            @Override
            public String api(ApiProducer<String> producer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String handleApiRequest(Context context, Request request) {
                return api;
            }

            @Override
            public void addDescriptorListener(Listener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void removeDescriptorListener(Listener listener) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Promise<Response, NeverThrowsException> handle(Context context, Request request) {
                throw new UnsupportedOperationException();
            }
        }
    }

    private static abstract class RouteMatchA implements RouteMatch {
    }

    private static abstract class RouteMatchB implements RouteMatch {
    }

    private interface DescribedHandler extends Handler, Describable<String, Request> {

    }

    private class StringApiProducer implements ApiProducer<String> {

        @Override
        public String withPath(String descriptor, String path) {
            return descriptor;
        }

        @Override
        public String withVersion(String descriptor, Version version) {
            return descriptor;
        }

        @Override
        public String merge(List<String> descriptors) {
            Collections.sort(descriptors);
            return descriptors.toString();
        }

        @Override
        public String addApiInfo(String descriptor) {
            return descriptor;
        }

        @Override
        public ApiProducer<String> newChildProducer(String idFragment) {
            return new StringApiProducer();
        }
    }


}
