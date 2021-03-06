package me.andrew28.addons.core;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import me.andrew28.addons.core.annotations.DoNotRegister;
import org.bukkit.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * @author Andrew Tran
 */
@DoNotRegister
public abstract class ASAEffect extends Effect implements AutoRegisteringSkriptElement, ExpressionManagerProvider {
    private ExpressionManager expressionManager = new ExpressionManager();
    private ElementSyntax[] elementSyntaxes;
    private boolean usesBinds = false;

    private Event currentEvent;
    private SkriptParser.ParseResult parseResult;

    public ElementSyntax[] getElementSyntaxes() {
        return elementSyntaxes;
    }

    public SkriptParser.ParseResult getParseResult() {
        return parseResult;
    }

    public Event getCurrentEvent() {
        return currentEvent;
    }

    public void setCurrentEvent(Event currentEvent) {
        this.currentEvent = currentEvent;
    }

    @Override
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedElementSyntax, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        elementSyntaxes = SyntaxElementUtil.getSyntax(getClass());
        this.parseResult = parseResult;
        for (ElementSyntax elementSyntax : elementSyntaxes) {
            if (elementSyntax != null && elementSyntax.isUsingBinds()) {
                usesBinds = true;
                break;
            }
        }
        if (usesBinds) {
            List<ElementSyntax> elementSyntaxesForBinds = new ArrayList<>();
            for (ElementSyntax elementSyntax : elementSyntaxes) {
                for (int i = 0; i < elementSyntax.getRawSyntaxes().length; i++) {
                    elementSyntaxesForBinds.add(elementSyntax);
                }
            }
            ElementSyntax matchedBindedSyntax = elementSyntaxesForBinds.get(matchedElementSyntax);
            if (matchedBindedSyntax.isUsingBinds()) {
                getExpressionManager().bind(matchedBindedSyntax, expressions);
            }
        }
        SkriptExpressionWrapper<?>[] skriptExpressionWrappers = Arrays.stream(expressions)
                .map((Function<Expression<?>, ? extends SkriptExpressionWrapper<?>>) SkriptExpressionWrapper::new)
                .toArray(SkriptExpressionWrapper[]::new);
        return init(skriptExpressionWrappers, matchedElementSyntax, kleenean, parseResult);
    }

    public boolean init(SkriptExpressionWrapper<?>[] expressions, int matchedPattern, Kleenean delay, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected void execute(Event event) {
        exp().setCurrentEvent(event);
        setCurrentEvent(event);
        try {
            execute();
        } catch (NullExpressionException e) {
            Skript.warning("Unexpected null value given to Expression " + getClass().getCanonicalName() + " (Message: " + e.getMessage() + ")");
        }
    }

    public abstract void execute() throws NullExpressionException;

    @Override
    public String toString(Event event, boolean b) {
        return "Andrew's Addon System Effect: " + getClass().getCanonicalName();
    }

    public void assertNotNull(Object object, String message) throws NullExpressionException {
        if (object == null) {
            throw new NullExpressionException(message);
        }
    }
}
