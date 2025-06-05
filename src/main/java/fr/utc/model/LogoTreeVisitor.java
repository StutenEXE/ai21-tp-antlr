package fr.utc.model;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.misc.Pair;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;

import fr.utc.gui.Traceur;
import fr.utc.parsing.LogoParser.AvContext;
import fr.utc.parsing.LogoParser.BcContext;
import fr.utc.parsing.LogoParser.ExprContext;
import fr.utc.parsing.LogoParser.FcapContext;
import fr.utc.parsing.LogoParser.FccContext;
import fr.utc.parsing.LogoParser.FloatContext;
import fr.utc.parsing.LogoParser.FposContext;
import fr.utc.parsing.LogoParser.LcContext;
import fr.utc.parsing.LogoParser.ParentheseContext;
import fr.utc.parsing.LogoParser.ReContext;
import fr.utc.parsing.LogoParser.TdContext;
import fr.utc.parsing.LogoParser.TgContext;
import javafx.beans.property.StringProperty;
import javafx.scene.paint.Color;

public class LogoTreeVisitor extends LogoStoppableTreeVisitor {
	fr.utc.gui.Traceur traceur;
	Log log;

	public LogoTreeVisitor() {
		traceur = new Traceur();
		log = new Log();
	}
	
	public StringProperty logProperty() {
		return log;
	}

	public fr.utc.gui.Traceur getTraceur() {
		return traceur;
	}

	/*
	 * Map des attributs associés à chaque noeud de l'arbre
	 * key = node, value = valeur de l'expression du node
	 */
	ParseTreeProperty<Double> atts = new ParseTreeProperty<Double>();

	public void setValue(ParseTree node, double value) {
		atts.put(node, value);
	}

	public double getValue(ParseTree node) {
		Double value = atts.get(node);
		if (value == null) {
			throw new NullPointerException();
		}
		return value;
	}

	// Instructions de base

	@Override
	public Integer visitTd(TdContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a droite");
		}
		return 0;
	}
	
	@Override
	public Integer visitTg(TgContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.td(-bilan.b);
			log.defaultLog(ctx);
			log.appendLog("Tourne de", String.valueOf(bilan.b), "a gauche");
		}
		return 0;
	}

	@Override
	public Integer visitAv(AvContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(bilan.b);
			// Différents type de log possibles. Voir classe Log
			log.defaultLog(ctx);
			log.appendLog("Avance de", String.valueOf(bilan.b));
		}
		return bilan.a;
	}
	
	@Override
	public Integer visitRe(ReContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			traceur.avance(-bilan.b);
			// Différents type de log possibles. Voir classe Log
			log.defaultLog(ctx);
			log.appendLog("Recule de", String.valueOf(bilan.b));
		}
		return bilan.a;
	}

	@Override
	public Integer visitLc(LcContext ctx) {
		traceur.leverCrayon();
		log.appendLog("Leve le crayon");
		return 0;
	}
	
	@Override
	public Integer visitBc(BcContext ctx) {
		traceur.baisserCrayon();
		log.appendLog("Baisse le crayon");
		return 0;
	}
	
	@Override
	public Integer visitFpos(FposContext ctx) {
		List<Pair<Integer, Double>> bilan = evaluate(ctx.expr());
		if (bilan.get(0).a == 0 && bilan.get(1).a == 0) {
			Double x = bilan.get(0).b;
			Double y = bilan.get(1).b;
			traceur.allerA(x, y);
			log.appendLog("Se deplace a", String.valueOf(x), String.valueOf(y));
		}
		return 0;
	}
	
	@Override
	public Integer visitFcc(FccContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		Color[] couleurs = { Color.BLACK, Color.RED, Color.GREEN, Color.YELLOW, Color.BLUE, Color.PURPLE, Color.LIGHTBLUE, Color.WHITE };
		if (bilan.a == 0) {
			Integer n = (int) (bilan.b % couleurs.length);
			traceur.changerCouleur(couleurs[n]);
			log.appendLog("Changement de couleur : rgb(", 
				String.valueOf(couleurs[n].getRed()), 
				String.valueOf(couleurs[n].getGreen()),
				String.valueOf(couleurs[n].getBlue()), ")"
			);
		}
		return 0;
	}
	
	@Override
	public Integer visitFcap(FcapContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			Double angle = bilan.b + 90;
			traceur.changerAngle(angle);
			log.appendLog("Changement d'angle :", String.valueOf(angle));

		}
		return 0;
	}
	
	// Expressions

	@Override
	public Integer visitFloat(FloatContext ctx) {
		String floatText = ctx.FLOAT().getText();
		setValue(ctx, Double.valueOf(floatText));
		return 0;
	}
	
	@Override 
	public Integer visitParenthese(ParentheseContext ctx) {
		Pair<Integer, Double> bilan = evaluate(ctx.expr());
		if (bilan.a == 0) {
			setValue(ctx, bilan.b);
		}
		return 0;
	}

	/**
	 * Visite le noeud expression
	 * S'il n'y a pas d'erreur (la valeur de retour de la visite vaut 0)
	 * on récupère la valeur de l'expressions à partir de la map
	 * sinon
	 * on affecte une valeur quelconque
	 * On retourne une paire, (code de visite, valeur)
	 * 
	 * @param expr
	 * @return
	 */
	private Pair<Integer, Double> evaluate(ParseTree expr) {
		int b = visit(expr);
		Double val = b == 0 ? getValue(expr) : Double.POSITIVE_INFINITY;
		return new Pair<Integer, Double>(b, val);
	}
		
	private List<Pair<Integer, Double>> evaluate(List<ExprContext> exprs) {
	    List<Pair<Integer, Double>> results = new ArrayList<>();
	    for (ExprContext expr : exprs) {
	        int b = visit(expr);
	        Double val = b == 0 ? getValue(expr) : Double.POSITIVE_INFINITY;
	        results.add(new Pair<>(b, val));
	    }
	    return results;
	}
}
